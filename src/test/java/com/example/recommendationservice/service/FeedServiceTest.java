package com.example.recommendationservice.service;

import com.example.recommendationservice.config.FeedProperties;
import com.example.recommendationservice.dto.FeedRequest;
import com.example.recommendationservice.dto.FeedResponse;
import com.example.recommendationservice.dto.PostSummaryDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeedServiceTest {

    @Mock private FeedScoringService feedScoringService;
    @Mock private FeedProperties feedProperties;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private ObjectMapper objectMapper;
    @Mock private Cursor<String> cursor;

    @InjectMocks
    private FeedService feedService;

    private FeedRequest feedRequest;
    private PostSummaryDto postDto;

    @BeforeEach
    void setUp() throws Exception {
        feedRequest = new FeedRequest();
        feedRequest.setUserId("user1");
        feedRequest.setPage(0);
        feedRequest.setSize(10);
        feedRequest.setExcludePostIds(Set.of());

        postDto = new PostSummaryDto();
        postDto.setPostId("post1");
        postDto.setAuthorId("author1");
        postDto.setText("Test post");
        postDto.setScore(0.9);
        postDto.setCreatedAt(Instant.now());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(feedProperties.getCandidateMultiplier()).thenReturn(3);
        when(feedProperties.getMaxCandidates()).thenReturn(300);
        when(objectMapper.writeValueAsString(any())).thenReturn("[{\"postId\":\"post1\"}]");
    }

    @Test
    void getFeed_returnsCachedFeedWhenAvailable() throws Exception {
        when(valueOperations.get("feed:user1:page:0")).thenReturn("[{\"postId\":\"post1\"}]");
        when(objectMapper.readValue(eq("[{\"postId\":\"post1\"}]"), any(TypeReference.class)))
                .thenReturn(List.of(postDto));

        FeedResponse result = feedService.getFeed(feedRequest);

        assertThat(result.getPosts()).hasSize(1);
        verify(feedScoringService, never()).buildFeed(any(), anyInt(), anySet());
    }

    @Test
    void getFeed_buildsNewFeedWhenCacheMiss() {
        when(valueOperations.get("feed:user1:page:0")).thenReturn(null);
        when(feedScoringService.buildFeed("user1", 30, Set.of())).thenReturn(List.of(postDto));

        FeedResponse result = feedService.getFeed(feedRequest);

        assertThat(result.getPosts()).hasSize(1);
        verify(feedScoringService).buildFeed("user1", 30, Set.of());
        verify(valueOperations).set(eq("feed:user1:page:0"), anyString(), eq(10L), any());
    }

    @Test
    void getFeed_paginatesResults() {
        when(valueOperations.get("feed:user1:page:0")).thenReturn(null);

        List<PostSummaryDto> posts = List.of(
                createPostDto("post1"), createPostDto("post2"),
                createPostDto("post3"), createPostDto("post4"),
                createPostDto("post5")
        );
        when(feedScoringService.buildFeed("user1", 30, Set.of())).thenReturn(posts);

        FeedResponse result = feedService.getFeed(feedRequest);

        assertThat(result.getPosts()).hasSize(5);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(5);
    }

    @Test
    void getFeed_respectsMaxCandidatesLimit() {
        feedRequest.setSize(50);
        when(valueOperations.get("feed:user1:page:0")).thenReturn(null);
        when(feedProperties.getMaxCandidates()).thenReturn(100);
        when(feedScoringService.buildFeed("user1", 100, Set.of())).thenReturn(List.of(postDto));

        feedService.getFeed(feedRequest);

        verify(feedScoringService).buildFeed("user1", 100, Set.of());
    }

    @Test
    void invalidateCache_deletesUserCacheKeys() {
        when(redisTemplate.scan(any())).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn("feed:user1:page:0", "feed:user1:page:1");

        feedService.invalidateCache("user1");

        verify(redisTemplate).delete(List.of("feed:user1:page:0", "feed:user1:page:1"));
    }

    @Test
    void invalidateCache_handlesNoKeys() {
        when(redisTemplate.scan(any())).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(false);

        feedService.invalidateCache("user1");

        verify(redisTemplate, never()).delete(any(Collection.class));
    }

    @Test
    void invalidateCache_handlesException() {
        when(redisTemplate.scan(any())).thenThrow(new RuntimeException("Redis error"));

        feedService.invalidateCache("user1");
    }

    private PostSummaryDto createPostDto(String id) {
        PostSummaryDto dto = new PostSummaryDto();
        dto.setPostId(id);
        dto.setAuthorId("author-" + id);
        dto.setText("Post " + id);
        dto.setScore(0.8);
        dto.setCreatedAt(Instant.now());
        return dto;
    }
}
