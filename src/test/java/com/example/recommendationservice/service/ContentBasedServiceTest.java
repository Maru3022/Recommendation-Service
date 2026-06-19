package com.example.recommendationservice.service;

import com.example.recommendationservice.config.FeedProperties;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentBasedServiceTest {

    @Mock private UserProfileService userProfileService;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private PostSearchRepository postSearchRepository;
    @Mock private co.elastic.clients.elasticsearch.ElasticsearchClient elasticsearchClient;
    @Mock private FeedProperties feedProperties;

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
    void getContentBasedPosts_returnsPostsFromPreferredCategoriesWhenNoEmbedding() {
        when(userProfileService.getInterestEmbedding("user1")).thenReturn(Optional.empty());

        UserProfile profile = new UserProfile();
        profile.setUserId("user1");
        profile.setPreferredCategories(List.of("FITNESS"));
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(profile));
        when(postSearchRepository.findByCategoryOrderByLikesCountDesc(eq("FITNESS"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testPost)));

        List<PostDoc> result = contentBasedService.getContentBasedPosts("user1", 10, Set.of());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("FITNESS");
    }

    @Test
    void getContentBasedPosts_excludesRequestedPostIds() {
        when(userProfileService.getInterestEmbedding("user1")).thenReturn(Optional.empty());

        UserProfile profile = new UserProfile();
        profile.setUserId("user1");
        profile.setPreferredCategories(List.of("FITNESS"));
        when(userProfileRepository.findById("user1")).thenReturn(Optional.of(profile));

        PostDoc post2 = new PostDoc();
        post2.setId("post2");
        post2.setCategory("FITNESS");
        when(postSearchRepository.findByCategoryOrderByLikesCountDesc(eq("FITNESS"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testPost, post2)));

        List<PostDoc> result = contentBasedService.getContentBasedPosts("user1", 10, Set.of("post1"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("post2");
    }

    @Test
    void getContentBasedPosts_noPreferencesReturnsEmptyList() {
        when(userProfileService.getInterestEmbedding("user1")).thenReturn(Optional.empty());
        when(userProfileRepository.findById("user1")).thenReturn(Optional.empty());

        List<PostDoc> result = contentBasedService.getContentBasedPosts("user1", 10, Set.of());

        assertThat(result).isEmpty();
        verify(postSearchRepository, never()).findByCategoryOrderByLikesCountDesc(any(), any());
    }
}
