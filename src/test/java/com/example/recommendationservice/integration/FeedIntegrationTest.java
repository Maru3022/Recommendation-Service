package com.example.recommendationservice.integration;

import com.example.recommendationservice.dto.FeedRequest;
import com.example.recommendationservice.dto.FeedResponse;
import com.example.recommendationservice.dto.PostSummaryDto;
import com.example.recommendationservice.model.PostAction;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.model.UserProfile;
import com.example.recommendationservice.repository.PostActionRepository;
import com.example.recommendationservice.repository.PostSearchRepository;
import com.example.recommendationservice.repository.UserProfileRepository;
import com.example.recommendationservice.repository.UserProfileSearchRepository;
import com.example.recommendationservice.service.*;
import org.junit.jupiter.api.Test;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class FeedIntegrationTest {

    @Autowired private FeedService feedService;
    @Autowired private CollaborativeFilteringService collaborativeFilteringService;
    @Autowired private ContentBasedService contentBasedService;
    @Autowired private SocialSignalService socialSignalService;
    @Autowired private PostActionService postActionService;
    @Autowired private UserProfileService userProfileService;

    @MockitoBean private FeedScoringService feedScoringService;
    @MockitoBean private TrendingService trendingService;
    @MockitoBean private SemanticSearchService semanticSearchService;
    @MockitoBean private PostSearchRepository postSearchRepository;
    @MockitoBean private UserProfileRepository userProfileRepository;
    @MockitoBean private UserProfileSearchRepository userProfileSearchRepository;
    @MockitoBean private ElasticsearchClient elasticsearchClient;
    @MockitoBean private PostActionRepository postActionRepository;
    @MockitoBean private EmbeddingService embeddingService;
    @MockitoBean private ElasticsearchOperations elasticsearchOperations;
    @MockitoBean private RedisTemplate<String, Object> redisTemplate;
    @MockitoBean private KafkaTemplate<String, Object> kafkaTemplate;

    private UserProfile testUser() {
        UserProfile user = new UserProfile();
        user.setUserId("test-user-1");
        user.setPreferredCategories(List.of("FITNESS"));
        user.setFollowingIds(List.of("author-1"));
        return user;
    }

    private PostDoc testPost() {
        PostDoc post = new PostDoc();
        post.setId("post-1");
        post.setAuthorId("author-1");
        post.setText("Test fitness post");
        post.setCategory("FITNESS");
        post.setPostType("POST");
        post.setTags(List.of("workout", "health"));
        post.setLikesCount(10);
        post.setCommentsCount(5);
        post.setSavesCount(2);
        post.setSharesCount(1);
        post.setCreatedAt(Instant.now());
        post.setEmbedding(new float[1536]);
        return post;
    }

    @Test
    void completeFeedFlow_personalizedFeed() {
        ValueOperations<String, Object> valueOps = org.mockito.Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        when(feedScoringService.buildFeed(eq("test-user-1"), anyInt(), anySet()))
                .thenReturn(List.of(PostSummaryDto.builder().postId("post-1").score(0.9).build()));

        FeedRequest request = new FeedRequest();
        request.setUserId("test-user-1");
        request.setPage(0);
        request.setSize(10);
        request.setExcludePostIds(Set.of());

        FeedResponse response = feedService.getFeed(request);

        assertThat(response).isNotNull();
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getPosts()).hasSize(1);
    }

    @Test
    void completeFeedFlow_trackActionAndInvalidateCache() {
        when(postActionRepository.save(any(PostAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postActionRepository.findByUserId("test-user-1")).thenReturn(List.of(
                PostAction.builder().userId("test-user-1").postId("post-1")
                        .actionType(PostAction.ActionType.LIKE).build()
        ));

        postActionService.trackAction("test-user-1", "post-1", "LIKE");
        feedService.invalidateCache("test-user-1");

        List<PostAction> actions = postActionService.getUserActionHistory("test-user-1");
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getActionType()).isEqualTo(PostAction.ActionType.LIKE);
    }

    @Test
    void completeFeedFlow_contentBasedRecommendations() {
        when(userProfileRepository.findById("test-user-1")).thenReturn(Optional.of(testUser()));
        when(postSearchRepository.findByCategoryOrderByLikesCountDesc(eq("FITNESS"), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(testPost())));

        List<PostDoc> contentPosts = contentBasedService.getContentBasedPosts("test-user-1", 5, Set.of());
        assertThat(contentPosts).hasSize(1);
    }

    @Test
    void completeFeedFlow_socialSignalFeed() {
        when(userProfileRepository.findById("test-user-1")).thenReturn(Optional.of(testUser()));
        when(postSearchRepository.findByAuthorIdIn(eq(List.of("author-1")), any()))
                .thenReturn(List.of(testPost()));

        List<PostDoc> socialPosts = socialSignalService.getPostsFromFollowing("test-user-1", 10, Set.of());
        assertThat(socialPosts).hasSize(1);
    }

    @Test
    void completeFeedFlow_collaborativeFiltering() {
        when(userProfileRepository.findById("test-user-1")).thenReturn(Optional.of(testUser()));

        List<PostDoc> collaborativePosts = collaborativeFilteringService.getCollaborativePosts(
                "test-user-1", 5, Set.of());
        assertThat(collaborativePosts).isNotNull();
    }

    @Test
    void completeFeedFlow_trendingPosts() {
        when(trendingService.getTrendingPosts(10)).thenReturn(List.of(testPost()));

        List<PostDoc> trendingPosts = trendingService.getTrendingPosts(10);
        assertThat(trendingPosts).hasSize(1);
    }

    @Test
    void completeFeedFlow_userProfileCreation() {
        when(userProfileRepository.findById("new-user")).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile newUser = userProfileService.getOrCreate("new-user");

        assertThat(newUser).isNotNull();
        assertThat(newUser.getUserId()).isEqualTo("new-user");
    }

    @Test
    void completeFeedFlow_postActions() {
        when(postActionRepository.save(any(PostAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postActionRepository.findByUserId("test-user-1")).thenReturn(List.of(
                PostAction.builder().userId("test-user-1").postId("post-1").actionType(PostAction.ActionType.VIEW).build(),
                PostAction.builder().userId("test-user-1").postId("post-1").actionType(PostAction.ActionType.LIKE).build(),
                PostAction.builder().userId("test-user-1").postId("post-1").actionType(PostAction.ActionType.SAVE).build()
        ));

        postActionService.trackAction("test-user-1", "post-1", "VIEW");
        postActionService.trackAction("test-user-1", "post-1", "LIKE");
        postActionService.trackAction("test-user-1", "post-1", "SAVE");

        List<PostAction> actions = postActionService.getUserActionHistory("test-user-1");
        assertThat(actions).hasSize(3);
    }
}
