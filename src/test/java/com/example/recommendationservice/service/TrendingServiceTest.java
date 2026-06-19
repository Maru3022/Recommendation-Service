package com.example.recommendationservice.service;

import com.example.recommendationservice.config.FeedProperties;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.repository.PostActionRepository;
import com.example.recommendationservice.repository.PostSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrendingServiceTest {

    @Mock private PostActionRepository postActionRepository;
    @Mock private PostSearchRepository postSearchRepository;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ZSetOperations<String, Object> zSetOperations;
    @Mock private FeedProperties feedProperties;

    @InjectMocks
    private TrendingService trendingService;

    private PostDoc testPost;

    @BeforeEach
    void setUp() {
        testPost = new PostDoc();
        testPost.setId("post1");
        testPost.setAuthorId("author1");
        testPost.setText("Test post");
        testPost.setLikesCount(100);
        testPost.setCommentsCount(20);
        testPost.setCreatedAt(Instant.now());
    }

    @Test
    void getTrendingPosts_returnsCachedPosts() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange("trending:posts", 0, 9)).thenReturn(Set.of("post1"));
        when(postSearchRepository.findByIdIn(List.of("post1"))).thenReturn(List.of(testPost));

        List<PostDoc> result = trendingService.getTrendingPosts(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("post1");
    }

    @Test
    void getTrendingPosts_refreshesWhenCacheEmpty() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange("trending:posts", 0, 0)).thenReturn(Set.of());
        when(feedProperties.getTrendingWindowHours()).thenReturn(72);
        when(postActionRepository.findTrendingPostIds(any(), eq(PageRequest.of(0, 200))))
                .thenReturn(List.<Object[]>of(new Object[]{"post1", 42L}));
        when(postSearchRepository.findByIdIn(List.of("post1"))).thenReturn(List.of(testPost));

        List<PostDoc> result = trendingService.getTrendingPosts(1);

        assertThat(result).hasSize(1);
        verify(zSetOperations).add("trending:posts", "post1", 42.0);
    }

    @Test
    void getTrendingPosts_emptyRepositoryReturnsEmptyList() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange("trending:posts", 0, 9)).thenReturn(null);
        when(feedProperties.getTrendingWindowHours()).thenReturn(72);
        when(postActionRepository.findTrendingPostIds(any(), eq(PageRequest.of(0, 200))))
                .thenReturn(List.of());

        List<PostDoc> result = trendingService.getTrendingPosts(10);

        assertThat(result).isEmpty();
    }
}
