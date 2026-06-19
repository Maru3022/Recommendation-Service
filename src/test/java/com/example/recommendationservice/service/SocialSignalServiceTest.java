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
class SocialSignalServiceTest {

    @Mock private PostSearchRepository postSearchRepository;
    @Mock private SocialGraphService socialGraphService;

    @InjectMocks
    private SocialSignalService socialSignalService;

    private PostDoc testPost;

    @BeforeEach
    void setUp() {
        testPost = new PostDoc();
        testPost.setId("post1");
        testPost.setAuthorId("author1");
        testPost.setText("Test post");
        testPost.setCreatedAt(Instant.now());
    }

    @Test
    void getPostsFromFollowing_returnsPostsFromFollowedUsers() {
        when(socialGraphService.getFollowing("user1")).thenReturn(Set.of("author1", "author2"));
        
        Page<PostDoc> page = new PageImpl<>(List.of(testPost));
        when(postSearchRepository.findByAuthorIdIn(eq(Set.of("author1", "author2")), any(Pageable.class)))
                .thenReturn(page);

        List<PostDoc> result = socialSignalService.getPostsFromFollowing("user1", 10, Set.of());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAuthorId()).isEqualTo("author1");
    }

    @Test
    void getPostsFromFollowing_excludesRequestedPostIds() {
        when(socialGraphService.getFollowing("user1")).thenReturn(Set.of("author1"));
        
        PostDoc post2 = new PostDoc();
        post2.setId("post2");
        post2.setAuthorId("author1");
        
        Page<PostDoc> page = new PageImpl<>(List.of(testPost, post2));
        when(postSearchRepository.findByAuthorIdIn(eq(Set.of("author1")), any(Pageable.class)))
                .thenReturn(page);

        List<PostDoc> result = socialSignalService.getPostsFromFollowing("user1", 10, Set.of("post1"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("post2");
    }

    @Test
    void getPostsFromFollowing_emptyFollowingReturnsEmptyList() {
        when(socialGraphService.getFollowing("user1")).thenReturn(Set.of());

        List<PostDoc> result = socialSignalService.getPostsFromFollowing("user1", 10, Set.of());

        assertThat(result).isEmpty();
        verify(postSearchRepository, never()).findByAuthorIdIn(any(), any());
    }
}
