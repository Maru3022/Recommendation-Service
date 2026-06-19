package com.example.recommendationservice.service;

import com.example.recommendationservice.model.UserProfile;
import com.example.recommendationservice.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialGraphServiceTest {

    @Mock private UserProfileRepository userProfileRepository;

    @InjectMocks
    private SocialGraphService socialGraphService;

    private UserProfile testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserProfile();
        testUser.setUserId("user1");
        testUser.setFollowingIds(List.of("user2", "user3"));
    }

    @Test
    void getFollowing_returnsFollowingIds() {
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(testUser));

        Set<String> result = socialGraphService.getFollowing("user1");

        assertThat(result).contains("user2", "user3");
    }

    @Test
    void getFollowing_returnsEmptyWhenUserNotFound() {
        when(userProfileRepository.findById("user1")).thenReturn(Optional.empty());

        Set<String> result = socialGraphService.getFollowing("user1");

        assertThat(result).isEmpty();
    }

    @Test
    void getFollowers_returnsFollowers() {
        UserProfile follower = new UserProfile();
        follower.setUserId("user2");
        follower.setFollowingIds(List.of("user1"));

        when(userProfileRepository.findFollowers("user1")).thenReturn(List.of(follower));

        List<String> result = socialGraphService.getFollowers("user1");

        assertThat(result).contains("user2");
    }

    @Test
    void addFollowing_addsToFollowingList() {
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(testUser));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testUser);

        socialGraphService.addFollowing("user1", "user4");

        assertThat(testUser.getFollowingIds()).contains("user4");
        verify(userProfileRepository).save(testUser);
    }

    @Test
    void removeFollowing_removesFromFollowingList() {
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(testUser));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testUser);

        socialGraphService.removeFollowing("user1", "user2");

        assertThat(testUser.getFollowingIds()).doesNotContain("user2");
        verify(userProfileRepository).save(testUser);
    }

    @Test
    void getMuted_returnsEmptySet() {
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(testUser));

        Set<String> result = socialGraphService.getMuted("user1");

        assertThat(result).isEmpty();
    }
}
