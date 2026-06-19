package com.example.recommendationservice.service;

import com.example.recommendationservice.model.CreatePostRequest;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.repository.PostSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostSearchRepository postSearchRepository;
    @Mock private EmbeddingService embeddingService;

    @InjectMocks
    private PostService postService;

    private PostDoc testPost;
    private CreatePostRequest createRequest;

    @BeforeEach
    void setUp() {
        testPost = new PostDoc();
        testPost.setId("post1");
        testPost.setAuthorId("author1");
        testPost.setText("Test post");
        testPost.setCreatedAt(Instant.now());

        createRequest = CreatePostRequest.builder()
                .authorId("author1")
                .authorDisplayName("Author One")
                .text("Test post")
                .postType("POST")
                .category("FITNESS")
                .tags(List.of("tag1"))
                .visibility("PUBLIC")
                .build();
    }

    @Test
    void createPost_savesPostWithEmbedding() {
        when(embeddingService.generateEmbeddingForPost(any(PostDoc.class)))
                .thenReturn(Optional.of(new float[1536]));
        when(postSearchRepository.save(any(PostDoc.class))).thenReturn(testPost);

        PostDoc result = postService.createPost(createRequest);

        assertThat(result.getAuthorId()).isEqualTo("author1");
        verify(postSearchRepository).save(any(PostDoc.class));
        verify(embeddingService).generateEmbeddingForPost(any(PostDoc.class));
    }

    @Test
    void getPost_returnsPost() {
        when(postSearchRepository.findById("post1")).thenReturn(Optional.of(testPost));

        Optional<PostDoc> result = postService.getPost("post1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("post1");
    }

    @Test
    void getPost_returnsEmptyWhenNotFound() {
        when(postSearchRepository.findById("post1")).thenReturn(Optional.empty());

        Optional<PostDoc> result = postService.getPost("post1");

        assertThat(result).isEmpty();
    }

    @Test
    void deletePost_deletesWhenAuthorMatches() {
        when(postSearchRepository.findById("post1")).thenReturn(Optional.of(testPost));

        boolean deleted = postService.deletePost("post1", "author1");

        assertThat(deleted).isTrue();
        verify(postSearchRepository).deleteById("post1");
    }

    @Test
    void deletePost_returnsFalseWhenNotAuthor() {
        when(postSearchRepository.findById("post1")).thenReturn(Optional.of(testPost));

        boolean deleted = postService.deletePost("post1", "other-user");

        assertThat(deleted).isFalse();
        verify(postSearchRepository, never()).deleteById(any());
    }
}
