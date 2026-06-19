package com.example.recommendationservice.service;

import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.repository.PostSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentBasedServiceTest {

    @Mock private PostSearchRepository postSearchRepository;
    @Mock private UserProfileService userProfileService;

    @InjectMocks
    private ContentBasedService contentBasedService;

    private PostDoc testPost;

    @BeforeEach
    void setUp() {
        testPost = new PostDoc();
        testPost.setId("post1");
        testPost.setAuthorId("author1");
        testPost.setText("Test post");
        testPost.setCategory("FITNESS");
        testPost.setCreatedAt(Instant.now());
    }

    @Test
    void getContentBasedPosts_returnsPostsFromPreferredCategories() {
        when(userProfileService.getOrCreate("user1")).thenAnswer(invocation -> {
            com.example.recommendationservice.model.UserProfile profile = 
                new com.example.recommendationservice.model.UserProfile();
            profile.setUserId("user1");
            profile.setPreferredCategories(List.of("FITNESS", "WORKOUT"));
            return profile;
        });

        Page<PostDoc> page = new PageImpl<>(List.of(testPost));
        when(postSearchRepository.findByCategory(eq("FITNESS"), any(Pageable.class))).thenReturn(page);

        List<PostDoc> result = contentBasedService.getContentBasedPosts("user1", 10, Set.of());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("FITNESS");
    }

    @Test
    void getContentBasedPosts_excludesRequestedPostIds() {
        when(userProfileService.getOrCreate("user1")).thenAnswer(invocation -> {
            com.example.recommendationservice.model.UserProfile profile = 
                new com.example.recommendationservice.model.UserProfile();
            profile.setUserId("user1");
            profile.setPreferredCategories(List.of("FITNESS"));
            return profile;
        });

        PostDoc post2 = new PostDoc();
        post2.setId("post2");
        post2.setCategory("FITNESS");

        Page<PostDoc> page = new PageImpl<>(List.of(testPost, post2));
        when(postSearchRepository.findByCategory(eq("FITNESS"), any(Pageable.class))).thenReturn(page);

        List<PostDoc> result = contentBasedService.getContentBasedPosts("user1", 10, Set.of("post1"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("post2");
    }

    @Test
    void getContentBasedPosts_noPreferencesReturnsEmptyList() {
        when(userProfileService.getOrCreate("user1")).thenAnswer(invocation -> {
            com.example.recommendationservice.model.UserProfile profile = 
                new com.example.recommendationservice.model.UserProfile();
            profile.setUserId("user1");
            profile.setPreferredCategories(List.of());
            return profile;
        });

        List<PostDoc> result = contentBasedService.getContentBasedPosts("user1", 10, Set.of());

        assertThat(result).isEmpty();
        verify(postSearchRepository, never()).findByCategory(any(), any());
    }
}
