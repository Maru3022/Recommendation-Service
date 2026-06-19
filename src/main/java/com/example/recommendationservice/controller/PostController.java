package com.example.recommendationservice.controller;

import com.example.recommendationservice.model.CreatePostRequest;
import com.example.recommendationservice.model.PostAction;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.model.TrackActionRequest;
import com.example.recommendationservice.service.PostActionService;
import com.example.recommendationservice.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@Validated
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
@Tag(name = "Post API", description = "Create, retrieve, delete posts and track user interactions")
public class PostController {

    private final PostService       postService;
    private final PostActionService postActionService;

    // -------------------------------------------------------------------------
    // Post CRUD
    // -------------------------------------------------------------------------

    @Operation(summary = "Create a new post")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Post created"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping
    public ResponseEntity<PostDoc> createPost(@Valid @RequestBody CreatePostRequest request) {
        log.info("Create post | author={} type={}", request.getAuthorId(), request.getPostType());
        PostDoc post = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    @Operation(summary = "Get a post by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Post found"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    @GetMapping("/{postId}")
    public ResponseEntity<PostDoc> getPost(
            @PathVariable @NotBlank @Size(max = 50) String postId) {

        return postService.getPost(postId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Delete a post",
            description = "Only the post author can delete their own post. Pass the authorId as a query param for authorisation.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "403", description = "Not the post author"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable @NotBlank @Size(max = 50) String postId,
            @RequestParam @NotBlank String requestingUserId) {

        boolean deleted = postService.deletePost(postId, requestingUserId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        // Either not found or not authorised — distinguish by checking presence
        boolean exists = postService.getPost(postId).isPresent();
        return exists
                ? ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                : ResponseEntity.notFound().build();
    }

    // -------------------------------------------------------------------------
    // Action tracking
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Track a user action on a post",
            description = "Action types: VIEW, LIKE, COMMENT, SHARE, SAVE, HIDE, REPORT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Action tracked"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping("/{postId}/actions")
    public ResponseEntity<PostAction> trackAction(
            @PathVariable @NotBlank @Size(max = 50) String postId,
            @Valid @RequestBody TrackActionRequest request) {

        log.info("Track action | post={} user={} type={}", postId, request.getUserId(), request.getActionType());
        PostAction action = postActionService.trackAction(postId, request.getUserId(), request.getActionType());
        return ResponseEntity.ok(action);
    }

    @Operation(
            summary = "Get user action history",
            description = "Returns all actions the user has performed on posts")
    @ApiResponse(responseCode = "200", description = "History returned")
    @GetMapping("/{userId}/history")
    public ResponseEntity<List<PostAction>> getUserHistory(
            @PathVariable @NotBlank @Size(max = 50) String userId) {

        log.info("Action history request | user={}", userId);
        return ResponseEntity.ok(postActionService.getUserActionHistory(userId));
    }
}
