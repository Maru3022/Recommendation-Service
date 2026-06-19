package com.example.recommendationservice.service;

import com.example.recommendationservice.config.FeedProperties;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.model.UserProfile;
import com.example.recommendationservice.model.UserProfileDoc;
import com.example.recommendationservice.repository.PostSearchRepository;
import com.example.recommendationservice.repository.UserProfileRepository;
import com.example.recommendationservice.repository.UserProfileSearchRepository;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CollaborativeFilteringServiceTest {

    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserProfileService userProfileService;
    @Mock private UserProfileSearchRepository userProfileSearchRepository;
    @Mock private PostSearchRepository postSearchRepository;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private ObjectMapper objectMapper;
    @Mock private FeedProperties feedProperties;

    @InjectMocks
    private CollaborativeFilteringService collaborativeFilteringService;

    private UserProfile testUser;
    private PostDoc testPost;

    @BeforeEach
    void setUp() {
        testUser = new UserProfile();
        testUser.setUserId("user1");
        testUser.setInterestEmbeddingJson("[0.1, 0.2]");
        testUser.setLikedPostIds(Set.of("post1", "post2"));
        testUser.setViewedPostIds(List.of("post3"));

        testPost = new PostDoc();
        testPost.setId("post1");
        testPost.setAuthorId("author1");
        testPost.setText("Test post");
        testPost.setCreatedAt(Instant.now());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(feedProperties.getSimilarUsersLimit()).thenReturn(5);
        when(feedProperties.getKnnCandidatesMultiplier()).thenReturn(10);
        when(feedProperties.getKnnMinCandidates()).thenReturn(50);
    }

    @Test
    void getCollaborativePosts_returnsPostsFromSimilarUsers() {
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(testUser));
        when(userProfileService.getInterestEmbedding("user1")).thenReturn(Optional.of(new float[1536]));
        
        UserProfileDoc similarUserDoc = new UserProfileDoc();
        similarUserDoc.setUserId("user2");
        when(userProfileSearchRepository.findSimilarByKnn(any(float[].class), eq(5), anyInt()))
                .thenReturn(List.of(similarUserDoc));

        UserProfile similarUserProfile = new UserProfile();
        similarUserProfile.setUserId("user2");
        similarUserProfile.setLikedPostIds(Set.of("post4", "post5"));
        when(userProfileRepository.findById("user2")).thenReturn(Optional.of(similarUserProfile));

        when(postSearchRepository.findByIdIn(anyList())).thenReturn(List.of(testPost));

        List<PostDoc> result = collaborativeFilteringService.getCollaborativePosts("user1", 10, Set.of());

        assertThat(result).isNotEmpty();
        verify(userProfileSearchRepository).findSimilarByKnn(any(float[].class), eq(5), anyInt());
    }

    @Test
    void getCollaborativePosts_userNotFound_returnsEmptyList() {
        when(userProfileRepository.findById("user1")).thenReturn(Optional.empty());

        List<PostDoc> result = collaborativeFilteringService.getCollaborativePosts("user1", 10, Set.of());

        assertThat(result).isEmpty();
        verify(userProfileSearchRepository, never()).findSimilarByKnn(any(), anyInt(), anyInt());
    }

    @Test
    void getCollaborativePosts_noEmbedding_returnsEmptyList() {
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(testUser));
        when(userProfileService.getInterestEmbedding("user1")).thenReturn(Optional.empty());

        List<PostDoc> result = collaborativeFilteringService.getCollaborativePosts("user1", 10, Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    void getCollaborativePosts_usesCachedSimilarUsers() throws Exception {
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(testUser));
        when(valueOperations.get("similar_users:user1")).thenReturn("[\"user2\"]");
        when(objectMapper.readValue(eq("[\"user2\"]"), any(TypeReference.class))).thenReturn(List.of("user2"));

        UserProfile similarUserProfile = new UserProfile();
        similarUserProfile.setUserId("user2");
        similarUserProfile.setLikedPostIds(Set.of("post4"));
        when(userProfileRepository.findById("user2")).thenReturn(Optional.of(similarUserProfile));

        PostDoc likedPost = new PostDoc();
        likedPost.setId("post4");
        when(postSearchRepository.findByIdIn(anyList())).thenReturn(List.of(likedPost));

        List<PostDoc> result = collaborativeFilteringService.getCollaborativePosts("user1", 10, Set.of());

        assertThat(result).isNotEmpty();
        verify(userProfileSearchRepository, never()).findSimilarByKnn(any(), anyInt(), anyInt());
    }

    @Test
    void refreshAllSimilarUsers_processesAllUsers() {
        UserProfile user2 = new UserProfile();
        user2.setUserId("user2");
        user2.setInterestEmbeddingJson("[0.1, 0.2]");

        when(userProfileRepository.findAll()).thenReturn(List.of(testUser, user2));
        when(userProfileService.getInterestEmbedding("user1")).thenReturn(Optional.of(new float[1536]));
        when(userProfileService.getInterestEmbedding("user2")).thenReturn(Optional.of(new float[1536]));

        UserProfileDoc doc1 = new UserProfileDoc();
        doc1.setUserId("user1");
        UserProfileDoc doc2 = new UserProfileDoc();
        doc2.setUserId("user2");

        when(userProfileSearchRepository.findSimilarByKnn(any(float[].class), eq(5), anyInt()))
                .thenReturn(List.of(doc1, doc2));

        collaborativeFilteringService.refreshAllSimilarUsers();

        verify(userProfileSearchRepository, times(2)).findSimilarByKnn(any(float[].class), eq(5), anyInt());
    }

    @Test
    void computeSimilarUsers_knnFailure_returnsEmptyList() {
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(testUser));
        when(userProfileService.getInterestEmbedding("user1")).thenReturn(Optional.of(new float[1536]));
        when(userProfileSearchRepository.findSimilarByKnn(any(float[].class), eq(5), anyInt()))
                .thenThrow(new RuntimeException("ES error"));

        List<PostDoc> result = collaborativeFilteringService.getCollaborativePosts("user1", 10, Set.of());

        assertThat(result).isEmpty();
    }
}
