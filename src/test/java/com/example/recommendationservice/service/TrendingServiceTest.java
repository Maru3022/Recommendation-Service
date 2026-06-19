package com.example.recommendationservice.service;

import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.repository.PostSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrendingServiceTest {

    @Mock private PostSearchRepository postSearchRepository;

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
    void getTrendingPosts_returnsMostEngagedPosts() {
        Page<PostDoc> page = new PageImpl<>(List.of(testPost));
        when(postSearchRepository.findAll(any(Pageable.class))).thenReturn(page);

        List<PostDoc> result = trendingService.getTrendingPosts(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("post1");
    }

    @Test
    void getTrendingPosts_respectsLimit() {
        PostDoc post2 = new PostDoc();
        post2.setId("post2");
        post2.setLikesCount(50);
        
        Page<PostDoc> page = new PageImpl<>(List.of(testPost, post2));
        when(postSearchRepository.findAll(any(Pageable.class))).thenReturn(page);

        List<PostDoc> result = trendingService.getTrendingPosts(1);

        assertThat(result).hasSize(1);
    }

    @Test
    void getTrendingPosts_emptyRepositoryReturnsEmptyList() {
        Page<PostDoc> page = new PageImpl<>(List.of());
        when(postSearchRepository.findAll(any(Pageable.class))).thenReturn(page);

        List<PostDoc> result = trendingService.getTrendingPosts(10);

        assertThat(result).isEmpty();
    }
}
