package com.example.recommendationservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialGraphServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private SetOperations<String, Object> setOperations;

    @InjectMocks
    private SocialGraphService socialGraphService;

    @Test
    void getFollowing_returnsFollowingIds() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("social:user1:following")).thenReturn(Set.of("user2", "user3"));

        Set<String> result = socialGraphService.getFollowing("user1");

        assertThat(result).containsExactlyInAnyOrder("user2", "user3");
    }

    @Test
    void getFollowing_returnsEmptyWhenNoData() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("social:user1:following")).thenReturn(null);

        Set<String> result = socialGraphService.getFollowing("user1");

        assertThat(result).isEmpty();
    }

    @Test
    void getFollowers_returnsFollowers() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("social:user1:followers")).thenReturn(Set.of("user2"));

        Set<String> result = socialGraphService.getFollowers("user1");

        assertThat(result).contains("user2");
    }

    @Test
    void follow_addsToFollowingAndFollowersSets() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        socialGraphService.follow("user1", "user2");

        verify(setOperations).add("social:user1:following", "user2");
        verify(setOperations).add("social:user2:followers", "user1");
    }

    @Test
    void follow_rejectsSelfFollow() {
        assertThatThrownBy(() -> socialGraphService.follow("user1", "user1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot follow themselves");
    }

    @Test
    void unfollow_removesFromFollowingAndFollowersSets() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        socialGraphService.unfollow("user1", "user2");

        verify(setOperations).remove("social:user1:following", "user2");
        verify(setOperations).remove("social:user2:followers", "user1");
    }

    @Test
    void getMuted_returnsEmptySetWhenUnset() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("social:user1:muted")).thenReturn(null);

        Set<String> result = socialGraphService.getMuted("user1");

        assertThat(result).isEmpty();
    }
}
