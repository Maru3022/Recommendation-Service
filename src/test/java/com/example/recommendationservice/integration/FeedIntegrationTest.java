package com.example.recommendationservice.integration;

import com.example.recommendationservice.dto.FeedRequest;
import com.example.recommendationservice.dto.FeedResponse;
import com.example.recommendationservice.dto.PostActionEvent;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.model.UserProfile;
import com.example.recommendationservice.repository.PostSearchRepository;
import com.example.recommendationservice.repository.UserProfileRepository;
import com.example.recommendationservice.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class FeedIntegrationTest {

    @Autowired private FeedService feedService;
    @Autowired private FeedScoringService feedScoringService;
    @Autowired private CollaborativeFilteringService collaborativeFilteringService;
    @Autowired private ContentBasedService contentBasedService;
    @Autowired private TrendingService trendingService;
    @Autowired private SocialSignalService socialSignalService;
    @Autowired private PostActionService postActionService;
    @Autowired private UserProfileService userProfileService;
    @Autowired private PostSearchRepository postSearchRepository;
    @Autowired private UserProfileRepository userProfileRepository;

    @MockBean private EmbeddingService embeddingService;
    @MockBean private ElasticsearchOperations elasticsearchOperations;
    @MockBean private RedisTemplate<String, Object> redisTemplate;
    @MockBean private KafkaTemplate<String, Object> kafkaTemplate;

    private UserProfile testUser;
    private PostDoc testPost;

    @BeforeEach
    void setUp() {
        postSearchRepository.deleteAll();
        userProfileRepository.deleteAll();

        testUser = new UserProfile();
        testUser.setUserId("test-user-1");
        testUser.setPreferredCategories(List.of("FITNESS"));
        testUser.setFollowingIds(List.of("author-1"));
        userProfileRepository.save(testUser);

        testPost = new PostDoc();
        testPost.setId("post-1");
        testPost.setAuthorId("author-1");
        testPost.setText("Test fitness post");
        testPost.setCategory("FITNESS");
        testPost.setPostType("POST");
        testPost.setTags(List.of("workout", "health"));
        testPost.setLikesCount(10);
        testPost.setCommentsCount(5);
        testPost.setSavesCount(2);
        testPost.setSharesCount(1);
        testPost.setCreatedAt(Instant.now());
        testPost.setEmbedding(new float[1536]);
        postSearchRepository.save(testPost);

        when(embeddingService.generateEmbedding(any())).thenReturn(new float[1536]);
    }

    @Test
    void completeFeedFlow_personalizedFeed() {
        FeedRequest request = new FeedRequest();
        request.setUserId("test-user-1");
        request.setPage(0);
        request.setSize(10);
        request.setExcludePostIds(Set.of());

        FeedResponse response = feedService.getFeed(request);

        assertThat(response).isNotNull();
        assertThat(response.getPage()).isEqualTo(0);
    }

    @Test
    void completeFeedFlow_trackActionAndInvalidateCache() {
        postActionService.trackAction("test-user-1", "post-1", "LIKE");

        feedService.invalidateCache("test-user-1");

        UserProfile updatedUser = userProfileRepository.findById("test-user-1").orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getLikedPostIds()).contains("post-1");
    }

    @Test
    void completeFeedFlow_contentBasedRecommendations() {
        userProfileService.updateInterestEmbedding("test-user-1", new float[1536]);

        List<PostDoc> contentPosts = contentBasedService.getContentBasedPosts("test-user-1", 5, Set.of());

        assertThat(contentPosts).isNotNull();
    }

    @Test
    void completeFeedFlow_socialSignalFeed() {
        List<PostDoc> socialPosts = socialSignalService.getPostsFromFollowing("test-user-1", 10, Set.of());

        assertThat(socialPosts).isNotNull();
    }

    @Test
    void completeFeedFlow_collaborativeFiltering() {
        UserProfile similarUser = new UserProfile();
        similarUser.setUserId("test-user-2");
        similarUser.setLikedPostIds(Set.of("post-1"));
        similarUser.setInterestEmbeddingJson("[0.1, 0.2]");
        userProfileRepository.save(similarUser);

        List<PostDoc> collaborativePosts = collaborativeFilteringService.getCollaborativePosts(
                "test-user-1", 5, Set.of());

        assertThat(collaborativePosts).isNotNull();
    }

    @Test
    void completeFeedFlow_trendingPosts() {
        List<PostDoc> trendingPosts = trendingService.getTrendingPosts(10);

        assertThat(trendingPosts).isNotNull();
    }

    @Test
    void completeFeedFlow_userProfileCreation() {
        UserProfile newUser = userProfileService.getOrCreate("new-user");

        assertThat(newUser).isNotNull();
        assertThat(newUser.getUserId()).isEqualTo("new-user");
    }

    @Test
    void completeFeedFlow_postActions() {
        postActionService.trackAction("test-user-1", "post-1", "VIEW");
        postActionService.trackAction("test-user-1", "post-1", "LIKE");
        postActionService.trackAction("test-user-1", "post-1", "SAVE");

        List<com.example.recommendationservice.model.PostAction> actions = 
                postActionService.getUserActionHistory("test-user-1");

        assertThat(actions).hasSize(3);
    }
}
