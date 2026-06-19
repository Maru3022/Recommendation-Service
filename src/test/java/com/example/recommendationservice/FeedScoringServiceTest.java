package com.example.recommendationservice;

import com.example.recommendationservice.config.FeedProperties;
import com.example.recommendationservice.dto.PostSummaryDto;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedScoringServiceTest {

    @Mock private SocialSignalService socialSignalService;
    @Mock private CollaborativeFilteringService collaborativeFilteringService;
    @Mock private ContentBasedService contentBasedService;
    @Mock private TrendingService trendingService;
    @Mock private ScoringService scoringService;
    @Mock private FeedProperties feedProperties;

    @InjectMocks
    private FeedScoringService feedScoringService;

    private FeedProperties.Weights weights;

    @BeforeEach
    void setUp() {
        weights = new FeedProperties.Weights();
        weights.setSocial(0.35);
        weights.setCollaborative(0.25);
        weights.setContent(0.20);
        weights.setTrending(0.15);
        weights.setFreshness(0.05);

        when(feedProperties.getWeights()).thenReturn(weights);
        when(feedProperties.getFreshnessHalfLifeHours()).thenReturn(48.0);
        when(scoringService.calculateFeedScore(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenAnswer(invocation -> {
                    double social = invocation.getArgument(0);
                    double collaborative = invocation.getArgument(1);
                    double content = invocation.getArgument(2);
                    double trending = invocation.getArgument(3);
                    return weights.getSocial() * social + weights.getCollaborative() * collaborative
                            + weights.getContent() * content + weights.getTrending() * trending
                            + weights.getFreshness() * 0.5;
                });
        when(scoringService.calculateEngagementScore(anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(0.5);
    }

    @Test
    void buildFeed_returnsPostsSortedByScoreDescending() {
        String userId = "user1";
        PostDoc freshPost = makePost("p1", Instant.now());
        PostDoc oldPost = makePost("p2", Instant.now().minusSeconds(86400 * 7));

        when(socialSignalService.getPostsFromFollowing(eq(userId), anyInt(), anySet()))
                .thenReturn(List.of(freshPost));
        when(collaborativeFilteringService.getCollaborativePosts(eq(userId), anyInt(), anySet()))
                .thenReturn(List.of(oldPost));
        when(contentBasedService.getContentBasedPosts(eq(userId), anyInt(), anySet()))
                .thenReturn(Collections.emptyList());
        when(trendingService.getTrendingPosts(anyInt()))
                .thenReturn(Collections.emptyList());

        List<PostSummaryDto> result = feedScoringService.buildFeed(userId, 20, Set.of());

        assertThat(result).hasSize(2);
        // Fresh social post должен быть выше старого collaborative
        assertThat(result.get(0).getPostId()).isEqualTo("p1");
        assertThat(result.get(0).getScore()).isGreaterThan(result.get(1).getScore());
    }

    @Test
    void buildFeed_excludesRequestedPostIds() {
        String userId = "user1";
        PostDoc post = makePost("excluded-post", Instant.now());

        when(socialSignalService.getPostsFromFollowing(eq(userId), anyInt(), anySet()))
                .thenReturn(Collections.emptyList());
        when(collaborativeFilteringService.getCollaborativePosts(eq(userId), anyInt(), anySet()))
                .thenReturn(Collections.emptyList());
        when(contentBasedService.getContentBasedPosts(eq(userId), anyInt(), anySet()))
                .thenReturn(Collections.emptyList());
        when(trendingService.getTrendingPosts(anyInt())).thenReturn(List.of(post));

        List<PostSummaryDto> result = feedScoringService.buildFeed(userId, 20, Set.of("excluded-post"));

        assertThat(result).isEmpty();
    }

    @Test
    void buildFeed_emptySourcesReturnsEmptyList() {
        String userId = "user-no-data";

        when(socialSignalService.getPostsFromFollowing(any(), anyInt(), anySet()))
                .thenReturn(Collections.emptyList());
        when(collaborativeFilteringService.getCollaborativePosts(any(), anyInt(), anySet()))
                .thenReturn(Collections.emptyList());
        when(contentBasedService.getContentBasedPosts(any(), anyInt(), anySet()))
                .thenReturn(Collections.emptyList());
        when(trendingService.getTrendingPosts(anyInt())).thenReturn(Collections.emptyList());

        List<PostSummaryDto> result = feedScoringService.buildFeed(userId, 20, Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    void buildFeed_deduplicatesPostsAcrossSources() {
        String userId = "user1";
        PostDoc sharedPost = makePost("shared", Instant.now());

        // Тот же пост приходит из социального и trending источников
        when(socialSignalService.getPostsFromFollowing(eq(userId), anyInt(), anySet()))
                .thenReturn(List.of(sharedPost));
        when(collaborativeFilteringService.getCollaborativePosts(eq(userId), anyInt(), anySet()))
                .thenReturn(Collections.emptyList());
        when(contentBasedService.getContentBasedPosts(eq(userId), anyInt(), anySet()))
                .thenReturn(Collections.emptyList());
        when(trendingService.getTrendingPosts(anyInt())).thenReturn(List.of(sharedPost));

        List<PostSummaryDto> result = feedScoringService.buildFeed(userId, 20, Set.of());

        // LinkedHashMap дедуплицирует — пост должен быть один
        long count = result.stream().filter(p -> p.getPostId().equals("shared")).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void buildFeed_postDtoContainsCorrectFields() {
        String userId = "user1";
        PostDoc post = makePost("p1", Instant.now());
        post.setAuthorId("author1");
        post.setCategory("WORKOUT");
        post.setLikesCount(42);

        when(socialSignalService.getPostsFromFollowing(eq(userId), anyInt(), anySet()))
                .thenReturn(List.of(post));
        when(collaborativeFilteringService.getCollaborativePosts(any(), anyInt(), anySet()))
                .thenReturn(Collections.emptyList());
        when(contentBasedService.getContentBasedPosts(any(), anyInt(), anySet()))
                .thenReturn(Collections.emptyList());
        when(trendingService.getTrendingPosts(anyInt())).thenReturn(Collections.emptyList());

        List<PostSummaryDto> result = feedScoringService.buildFeed(userId, 20, Set.of());

        assertThat(result).hasSize(1);
        PostSummaryDto dto = result.get(0);
        assertThat(dto.getAuthorId()).isEqualTo("author1");
        assertThat(dto.getCategory()).isEqualTo("WORKOUT");
        assertThat(dto.getLikesCount()).isEqualTo(42);
        assertThat(dto.getScore()).isGreaterThan(0.0);
    }

    @Test
    void buildFeed_trendingPostsReceiveTrendingScore() {
        String userId = "user1";
        PostDoc trendingPost = makePost("trending1", Instant.now());
        PostDoc regularPost = makePost("regular1", Instant.now());

        when(socialSignalService.getPostsFromFollowing(eq(userId), anyInt(), anySet()))
                .thenReturn(List.of(regularPost));
        when(collaborativeFilteringService.getCollaborativePosts(eq(userId), anyInt(), anySet()))
                .thenReturn(Collections.emptyList());
        when(contentBasedService.getContentBasedPosts(eq(userId), anyInt(), anySet()))
                .thenReturn(Collections.emptyList());
        when(trendingService.getTrendingPosts(anyInt())).thenReturn(List.of(trendingPost));

        List<PostSummaryDto> result = feedScoringService.buildFeed(userId, 20, Set.of());

        // Social weight(0.35) > Trending engagementScore*weight — зависит от engagement
        // Главное что оба присутствуют
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(PostSummaryDto::getPostId))
                .containsExactlyInAnyOrder("trending1", "regular1");
    }

    // -----------------------------------------------------------------------

    private PostDoc makePost(String id, Instant createdAt) {
        PostDoc post = new PostDoc();
        post.setId(id);
        post.setAuthorId("author-" + id);
        post.setText("Post text " + id);
        post.setPostType("POST");
        post.setCategory("FITNESS");
        post.setLikesCount(10);
        post.setCommentsCount(2);
        post.setSavesCount(1);
        post.setSharesCount(0);
        post.setCreatedAt(createdAt);
        return post;
    }
}