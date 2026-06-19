package com.example.recommendationservice.controller;

import com.example.recommendationservice.dto.FeedRequest;
import com.example.recommendationservice.dto.FeedResponse;
import com.example.recommendationservice.dto.PostActionEvent;
import com.example.recommendationservice.model.PostAction;
import com.example.recommendationservice.repository.PostSearchRepository;
import com.example.recommendationservice.service.ContentBasedService;
import com.example.recommendationservice.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Feed", description = "Recommendation feed endpoints")
public class FeedController {

    private final FeedService feedService;
    private final ContentBasedService contentBasedService;
    private final PostSearchRepository postSearchRepository;

    @GetMapping
    @Operation(summary = "Get personalized recommendation feed")
    public ResponseEntity<FeedResponse> getFeed(
            @RequestParam @NotBlank String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Set<String> excludePostIds
    ) {
        FeedRequest request = new FeedRequest();
        request.setUserId(userId);
        request.setPage(page);
        request.setSize(size);
        request.setExcludePostIds(excludePostIds != null ? excludePostIds : Collections.emptySet());

        FeedResponse response = feedService.getFeed(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/action")
    @Operation(summary = "Record user action on a post")
    public ResponseEntity<Void> recordAction(@RequestBody @Valid PostActionEvent event) {
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        feedService.invalidateCache(event.getUserId());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Semantic search for posts")
    public ResponseEntity<FeedResponse> semanticSearch(
            @RequestParam @NotBlank String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<com.example.recommendationservice.dto.PostSummaryDto> posts =
                contentBasedService.getContentBasedPosts(userId, size, Collections.emptySet())
                        .stream()
                        .map(p -> com.example.recommendationservice.dto.PostSummaryDto.builder()
                                .postId(p.getId())
                                .authorId(p.getAuthorId())
                                .text(p.getText())
                                .postType(p.getPostType())
                                .category(p.getCategory())
                                .tags(p.getTags())
                                .likesCount(p.getLikesCount())
                                .commentsCount(p.getCommentsCount())
                                .createdAt(p.getCreatedAt())
                                .build())
                        .toList();

        FeedResponse response = FeedResponse.builder()
                .posts(posts)
                .page(page)
                .size(posts.size())
                .hasMore(false)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invalidate")
    @Operation(summary = "Invalidate feed cache for user")
    public ResponseEntity<Void> invalidateCache(@RequestParam @NotBlank String userId) {
        feedService.invalidateCache(userId);
        return ResponseEntity.noContent().build();
    }
}