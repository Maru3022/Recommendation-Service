package com.example.recommendationservice.service;

import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.model.UserProfile;
import com.example.recommendationservice.repository.PostSearchRepository;
import com.example.recommendationservice.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialSignalServiceTest {

    @Mock private UserProfileRepository userProfileRepository;
    @Mock private PostSearchRepository postSearchRepository;

    @InjectMocks
    private SocialSignalService socialSignalService;

    private PostDoc testPost;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        testPost = new PostDoc();
        testPost.setId("post1");
        testPost.setAuthorId("author1");
        testPost.setText("Test post");
        testPost.setCreatedAt(Instant.now());

        userProfile = new UserProfile();
        userProfile.setUserId("user1");
        userProfile.setFollowingIds(List.of("author1", "author2"));
        userProfile.setViewedPostIds(new ArrayList<>());
    }

    @Test
    void getPostsFromFollowing_returnsPostsFromFollowedUsers() {
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(userProfile));
        when(postSearchRepository.findByAuthorIdIn(eq(List.of("author1", "author2")), any(Pageable.class)))
                .thenReturn(List.of(testPost));

        List<PostDoc> result = socialSignalService.getPostsFromFollowing("user1", 10, Set.of());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAuthorId()).isEqualTo("author1");
    }

    @Test
    void getPostsFromFollowing_excludesRequestedPostIds() {
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(userProfile));

        PostDoc post2 = new PostDoc();
        post2.setId("post2");
        post2.setAuthorId("author1");
        when(postSearchRepository.findByAuthorIdIn(eq(List.of("author1", "author2")), any(Pageable.class)))
                .thenReturn(List.of(testPost, post2));

        List<PostDoc> result = socialSignalService.getPostsFromFollowing("user1", 10, Set.of("post1"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("post2");
    }

    @Test
    void getPostsFromFollowing_emptyFollowingReturnsEmptyList() {
        userProfile.setFollowingIds(List.of());
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(userProfile));

        List<PostDoc> result = socialSignalService.getPostsFromFollowing("user1", 10, Set.of());

        assertThat(result).isEmpty();
        verify(postSearchRepository, never()).findByAuthorIdIn(any(), any());
    }
}
