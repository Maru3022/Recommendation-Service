package com.example.recommendationservice.controller;

import com.example.recommendationservice.model.CreatePostRequest;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock private PostService postService;

    @InjectMocks
    private PostController postController;

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
    void createPost_returnsCreatedPost() {
        CreatePostRequest request = new CreatePostRequest();
        request.setText("Test post");
        request.setPostType("POST");
        request.setCategory("FITNESS");

        when(postService.createPost(any(), any(), any(), any(), any())).thenReturn(testPost);

        ResponseEntity<PostDoc> result = postController.createPost("author1", request);

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo("post1");
    }

    @Test
    void getPost_returnsPost() {
        when(postService.getPostById("post1")).thenReturn(Optional.of(testPost));

        ResponseEntity<PostDoc> result = postController.getPost("post1");

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo("post1");
    }

    @Test
    void getPost_returnsNotFoundWhenMissing() {
        when(postService.getPostById("post1")).thenReturn(Optional.empty());

        ResponseEntity<PostDoc> result = postController.getPost("post1");

        assertThat(result.getStatusCodeValue()).isEqualTo(404);
    }

    @Test
    void getPostsByAuthor_returnsPosts() {
        when(postService.getPostsByAuthor("author1")).thenReturn(List.of(testPost));

        ResponseEntity<List<PostDoc>> result = postController.getPostsByAuthor("author1");

        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void deletePost_returnsNoContent() {
        ResponseEntity<Void> result = postController.deletePost("post1");

        assertThat(result.getStatusCodeValue()).isEqualTo(204);
    }
}
