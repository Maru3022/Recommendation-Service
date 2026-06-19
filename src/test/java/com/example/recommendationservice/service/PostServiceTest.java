package com.example.recommendationservice.service;

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

    @BeforeEach
    void setUp() {
        testPost = new PostDoc();
        testPost.setId("post1");
        testPost.setAuthorId("author1");
        testPost.setText("Test post");
        testPost.setCreatedAt(Instant.now());
    }

    @Test
    void createPost_savesPostWithEmbedding() {
        when(embeddingService.generateEmbedding("Test post")).thenReturn(new float[1536]);
        when(postSearchRepository.save(any(PostDoc.class))).thenReturn(testPost);

        PostDoc result = postService.createPost("author1", "Test post", "POST", "FITNESS", List.of("tag1"));

        assertThat(result.getAuthorId()).isEqualTo("author1");
        verify(postSearchRepository).save(any(PostDoc.class));
        verify(embeddingService).generateEmbedding("Test post");
    }

    @Test
    void getPostById_returnsPost() {
        when(postSearchRepository.findById("post1")).thenReturn(Optional.of(testPost));

        Optional<PostDoc> result = postService.getPostById("post1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("post1");
    }

    @Test
    void getPostById_returnsEmptyWhenNotFound() {
        when(postSearchRepository.findById("post1")).thenReturn(Optional.empty());

        Optional<PostDoc> result = postService.getPostById("post1");

        assertThat(result).isEmpty();
    }

    @Test
    void getPostsByAuthor_returnsPosts() {
        when(postSearchRepository.findByAuthorId("author1")).thenReturn(List.of(testPost));

        List<PostDoc> result = postService.getPostsByAuthor("author1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAuthorId()).isEqualTo("author1");
    }

    @Test
    void deletePost_deletesPost() {
        doNothing().when(postSearchRepository).deleteById("post1");

        postService.deletePost("post1");

        verify(postSearchRepository).deleteById("post1");
    }
}
