package com.example.recommendationservice.controller;

import com.example.recommendationservice.dto.FeedRequest;
import com.example.recommendationservice.dto.FeedResponse;
import com.example.recommendationservice.dto.PostActionEvent;
import com.example.recommendationservice.dto.PostSummaryDto;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Feed", description = "Recommendation feed endpoints")
public class FeedController {

    private final FeedService feedService;
    private final FeedRankingService feedRankingService;
    private final CollaborativeFilteringService collaborativeFilteringService;
    private final ContentBasedService contentBasedService;
    private final TrendingService trendingService;
    private final SocialSignalService socialSignalService;
    private final PostActionService postActionService;

    @GetMapping("/personalized")
    @Operation(summary = "Get personalized recommendation feed (hybrid approach)")
    public ResponseEntity<FeedResponse> getPersonalizedFeed(
            @Parameter(description = "User ID") @RequestParam @NotBlank String userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Post IDs to exclude") @RequestParam(required = false) Set<String> excludePostIds
    ) {
        log.info("Getting personalized feed for user {}, page {}, size {}", userId, page, size);
        
        FeedRequest request = new FeedRequest();
        request.setUserId(userId);
        request.setPage(page);
        request.setSize(size);
        request.setExcludePostIds(excludePostIds != null ? excludePostIds : Collections.emptySet());

        FeedResponse response = feedService.getFeed(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/following")
    @Operation(summary = "Get feed from followed users (chronological)")
    public ResponseEntity<FeedResponse> getFollowingFeed(
            @Parameter(description = "User ID") @RequestParam @NotBlank String userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        log.info("Getting following feed for user {}, page {}, size {}", userId, page, size);
        
        com.example.recommendationservice.model.FeedResponse response = 
                feedRankingService.getFollowingFeed(userId, page, size);
        
        return ResponseEntity.ok(convertToDtoResponse(response, page));
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending posts feed")
    public ResponseEntity<FeedResponse> getTrendingFeed(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        log.info("Getting trending feed, page {}, size {}", page, size);
        
        List<PostDoc> trendingPosts = trendingService.getTrendingPosts(size);
        List<PostSummaryDto> posts = convertToPostSummaryDtos(trendingPosts);
        
        FeedResponse response = FeedResponse.builder()
                .posts(posts)
                .page(page)
                .size(posts.size())
                .hasMore(false)
                .build();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/collaborative")
    @Operation(summary = "Get collaborative filtering recommendations")
    public ResponseEntity<FeedResponse> getCollaborativeFeed(
            @Parameter(description = "User ID") @RequestParam @NotBlank String userId,
            @Parameter(description = "Limit") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @Parameter(description = "Post IDs to exclude") @RequestParam(required = false) Set<String> excludePostIds
    ) {
        log.info("Getting collaborative feed for user {}, limit {}", userId, limit);
        
        List<PostDoc> posts = collaborativeFilteringService.getCollaborativePosts(
                userId, limit, excludePostIds != null ? excludePostIds : Collections.emptySet());
        List<PostSummaryDto> postDtos = convertToPostSummaryDtos(posts);
        
        FeedResponse response = FeedResponse.builder()
                .posts(postDtos)
                .page(0)
                .size(postDtos.size())
                .hasMore(false)
                .build();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/content-based")
    @Operation(summary = "Get content-based recommendations")
    public ResponseEntity<FeedResponse> getContentBasedFeed(
            @Parameter(description = "User ID") @RequestParam @NotBlank String userId,
            @Parameter(description = "Limit") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @Parameter(description = "Post IDs to exclude") @RequestParam(required = false) Set<String> excludePostIds
    ) {
        log.info("Getting content-based feed for user {}, limit {}", userId, limit);
        
        List<PostDoc> posts = contentBasedService.getContentBasedPosts(
                userId, limit, excludePostIds != null ? excludePostIds : Collections.emptySet());
        List<PostSummaryDto> postDtos = convertToPostSummaryDtos(posts);
        
        FeedResponse response = FeedResponse.builder()
                .posts(postDtos)
                .page(0)
                .size(postDtos.size())
                .hasMore(false)
                .build();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/social")
    @Operation(summary = "Get posts from followed users")
    public ResponseEntity<FeedResponse> getSocialFeed(
            @Parameter(description = "User ID") @RequestParam @NotBlank String userId,
            @Parameter(description = "Limit") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @Parameter(description = "Post IDs to exclude") @RequestParam(required = false) Set<String> excludePostIds
    ) {
        log.info("Getting social feed for user {}, limit {}", userId, limit);
        
        List<PostDoc> posts = socialSignalService.getPostsFromFollowing(
                userId, limit, excludePostIds != null ? excludePostIds : Collections.emptySet());
        List<PostSummaryDto> postDtos = convertToPostSummaryDtos(posts);
        
        FeedResponse response = FeedResponse.builder()
                .posts(postDtos)
                .page(0)
                .size(postDtos.size())
                .hasMore(false)
                .build();
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/action")
    @Operation(summary = "Record user action on a post")
    public ResponseEntity<Void> recordAction(@RequestBody @Valid PostActionEvent event) {
        log.info("Recording action {} for user {} on post {}", event.getActionType(), event.getUserId(), event.getPostId());
        
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        
        postActionService.trackAction(event.getUserId(), event.getPostId(), event.getActionType().name());
        feedService.invalidateCache(event.getUserId());
        
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/invalidate")
    @Operation(summary = "Invalidate feed cache for user")
    public ResponseEntity<Void> invalidateCache(@Parameter(description = "User ID") @RequestParam @NotBlank String userId) {
        log.info("Invalidating cache for user {}", userId);
        feedService.invalidateCache(userId);
        return ResponseEntity.noContent().build();
    }

    private FeedResponse convertToDtoResponse(com.example.recommendationservice.model.FeedResponse modelResponse, int page) {
        List<PostSummaryDto> posts = modelResponse.getPosts().stream()
                .map(rp -> convertToPostSummaryDto(rp.getPost(), rp.getScore()))
                .collect(Collectors.toList());
        
        return FeedResponse.builder()
                .posts(posts)
                .page(page)
                .size(posts.size())
                .hasMore(modelResponse.isHasNext())
                .build();
    }

    private List<PostSummaryDto> convertToPostSummaryDtos(List<PostDoc> posts) {
        return posts.stream()
                .map(p -> convertToPostSummaryDto(p, 0.0))
                .collect(Collectors.toList());
    }

    private PostSummaryDto convertToPostSummaryDto(PostDoc post, double score) {
        return PostSummaryDto.builder()
                .postId(post.getId())
                .authorId(post.getAuthorId())
                .text(post.getText())
                .postType(post.getPostType())
                .category(post.getCategory())
                .tags(post.getTags())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .sharesCount(post.getSharesCount())
                .savesCount(post.getSavesCount())
                .score(score)
                .createdAt(post.getCreatedAt())
                .build();
    }
}