package com.example.recommendationservice.service;

import com.example.recommendationservice.model.UserProfile;
import com.example.recommendationservice.repository.UserProfileRepository;
import com.example.recommendationservice.repository.UserProfileSearchRepository;
import com.example.recommendationservice.model.UserProfileDoc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserProfileSearchRepository userProfileSearchRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userProfileService, "alphaPercent", 30);
    }

    @Test
    void getOrCreate_returnsExistingProfile() {
        UserProfile existingProfile = new UserProfile();
        existingProfile.setUserId("user1");
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(existingProfile));

        UserProfile result = userProfileService.getOrCreate("user1");

        assertThat(result).isEqualTo(existingProfile);
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void getOrCreate_createsNewProfileWhenNotExists() {
        when(userProfileRepository.findById("user1")).thenReturn(Optional.empty());
        UserProfile newProfile = new UserProfile();
        newProfile.setUserId("user1");
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(newProfile);

        UserProfile result = userProfileService.getOrCreate("user1");

        assertThat(result.getUserId()).isEqualTo("user1");
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void updateInterestEmbedding_createsNewEmbedding() {
        UserProfile profile = new UserProfile();
        profile.setUserId("user1");
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profile);

        float[] newEmbedding = new float[1536];
        userProfileService.updateInterestEmbedding("user1", newEmbedding);

        verify(userProfileRepository).save(any(UserProfile.class));
        verify(userProfileSearchRepository).save(any(UserProfileDoc.class));
    }

    @Test
    void updateInterestEmbedding_updatesExistingEmbedding() {
        UserProfile profile = new UserProfile();
        profile.setUserId("user1");
        profile.setInterestEmbeddingJson("[0.1, 0.2, 0.3]");
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profile);

        float[] newEmbedding = new float[3];
        newEmbedding[0] = 0.4f;
        newEmbedding[1] = 0.5f;
        newEmbedding[2] = 0.6f;

        userProfileService.updateInterestEmbedding("user1", newEmbedding);

        verify(userProfileRepository).save(any(UserProfile.class));
        verify(userProfileSearchRepository).save(any(UserProfileDoc.class));
    }

    @Test
    void recordLike_addsPostToLikedPosts() {
        UserProfile profile = new UserProfile();
        profile.setUserId("user1");
        profile.setLikedPostIds(Set.of());
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profile);

        userProfileService.recordLike("user1", "post1");

        assertThat(profile.getLikedPostIds()).contains("post1");
        verify(userProfileRepository).save(profile);
    }

    @Test
    void recordSave_addsPostToSavedPosts() {
        UserProfile profile = new UserProfile();
        profile.setUserId("user1");
        profile.setSavedPostIds(Set.of());
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profile);

        userProfileService.recordSave("user1", "post1");

        assertThat(profile.getSavedPostIds()).contains("post1");
        verify(userProfileRepository).save(profile);
    }

    @Test
    void recordView_addsPostToViewedPosts() {
        UserProfile profile = new UserProfile();
        profile.setUserId("user1");
        profile.setViewedPostIds(List.of());
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profile);

        userProfileService.recordView("user1", "post1");

        assertThat(profile.getViewedPostIds()).contains("post1");
        verify(userProfileRepository).save(profile);
    }

    @Test
    void recordView_respectsMaxViewedPostsLimit() {
        UserProfile profile = new UserProfile();
        profile.setUserId("user1");
        // Fill up to max limit
        for (int i = 0; i < 500; i++) {
            profile.addViewedPost("post" + i);
        }
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profile);

        userProfileService.recordView("user1", "post501");

        assertThat(profile.getViewedPostIds()).hasSize(500);
        assertThat(profile.getViewedPostIds()).doesNotContain("post0");
        assertThat(profile.getViewedPostIds()).contains("post501");
    }

    @Test
    void getInterestEmbedding_returnsEmbeddingWhenExists() {
        UserProfile profile = new UserProfile();
        profile.setInterestEmbeddingJson("[0.1, 0.2, 0.3]");
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(profile));

        Optional<float[]> result = userProfileService.getInterestEmbedding("user1");

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
    }

    @Test
    void getInterestEmbedding_returnsEmptyWhenNotExists() {
        when(userProfileRepository.findById("user1")).thenReturn(Optional.empty());

        Optional<float[]> result = userProfileService.getInterestEmbedding("user1");

        assertThat(result).isEmpty();
    }

    @Test
    void syncToElasticsearch_handlesException() {
        UserProfile profile = new UserProfile();
        profile.setUserId("user1");
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profile);
        when(userProfileSearchRepository.save(any(UserProfileDoc.class)))
                .thenThrow(new RuntimeException("ES error"));

        float[] newEmbedding = new float[1536];
        
        // Should not throw exception
        userProfileService.updateInterestEmbedding("user1", newEmbedding);
    }
}
