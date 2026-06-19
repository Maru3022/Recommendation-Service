package com.example.recommendationservice.controller;

import com.example.recommendationservice.model.CreatePostRequest;
import com.example.recommendationservice.model.PostAction;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.model.TrackActionRequest;
import com.example.recommendationservice.service.PostActionService;
import com.example.recommendationservice.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock private PostService postService;
    @Mock private PostActionService postActionService;

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
        CreatePostRequest request = CreatePostRequest.builder()
                .authorId("author1")
                .authorDisplayName("Author One")
                .text("Test post")
                .postType("POST")
                .category("FITNESS")
                .visibility("PUBLIC")
                .build();

        when(postService.createPost(any(CreatePostRequest.class))).thenReturn(testPost);

        ResponseEntity<PostDoc> result = postController.createPost(request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo("post1");
    }

    @Test
    void getPost_returnsPost() {
        when(postService.getPost("post1")).thenReturn(Optional.of(testPost));

        ResponseEntity<PostDoc> result = postController.getPost("post1");

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo("post1");
    }

    @Test
    void getPost_returnsNotFoundWhenMissing() {
        when(postService.getPost("post1")).thenReturn(Optional.empty());

        ResponseEntity<PostDoc> result = postController.getPost("post1");

        assertThat(result.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void deletePost_returnsNoContentWhenAuthorised() {
        when(postService.deletePost("post1", "author1")).thenReturn(true);

        ResponseEntity<Void> result = postController.deletePost("post1", "author1");

        assertThat(result.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void deletePost_returnsForbiddenWhenNotAuthor() {
        when(postService.deletePost("post1", "other-user")).thenReturn(false);
        when(postService.getPost("post1")).thenReturn(Optional.of(testPost));

        ResponseEntity<Void> result = postController.deletePost("post1", "other-user");

        assertThat(result.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void trackAction_returnsTrackedAction() {
        TrackActionRequest request = new TrackActionRequest("user1", "LIKE");
        PostAction action = PostAction.builder()
                .userId("user1")
                .postId("post1")
                .actionType(PostAction.ActionType.LIKE)
                .build();
        when(postActionService.trackAction("post1", "user1", "LIKE")).thenReturn(action);

        ResponseEntity<PostAction> result = postController.trackAction("post1", request);

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getActionType()).isEqualTo(PostAction.ActionType.LIKE);
    }
}
