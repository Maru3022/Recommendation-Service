package com.example.recommendationservice.controller;

import com.example.recommendationservice.service.SocialGraphService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialControllerTest {

    @Mock private SocialGraphService socialGraphService;

    @InjectMocks
    private SocialController socialController;

    @Test
    void getFollowing_returnsFollowingSet() {
        when(socialGraphService.getFollowing("user1")).thenReturn(Set.of("user2", "user3"));

        ResponseEntity<Set<String>> result = socialController.getFollowing("user1");

        assertThat(result.getBody()).contains("user2", "user3");
    }

    @Test
    void getFollowers_returnsFollowersSet() {
        when(socialGraphService.getFollowers("user1")).thenReturn(Set.of("user2", "user3"));

        ResponseEntity<Set<String>> result = socialController.getFollowers("user1");

        assertThat(result.getBody()).contains("user2", "user3");
    }

    @Test
    void follow_returnsOk() {
        ResponseEntity<Void> result = socialController.follow("user1", "user2");

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        verify(socialGraphService).follow("user1", "user2");
    }

    @Test
    void unfollow_returnsNoContent() {
        ResponseEntity<Void> result = socialController.unfollow("user1", "user2");

        assertThat(result.getStatusCode().value()).isEqualTo(204);
        verify(socialGraphService).unfollow("user1", "user2");
    }
}
