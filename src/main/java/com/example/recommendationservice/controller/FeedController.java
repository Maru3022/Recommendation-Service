package com.example.recommendationservice.controller;

import com.example.recommendationservice.model.FeedResponse;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.service.FeedRankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
@Validated
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
@Tag(name = "Feed API", description = "Personalized fitness post feed with hybrid ranking")
public class FeedController {

    private final FeedRankingService feedRankingService;

    // -------------------------------------------------------------------------
    // Main personalised feed
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get personalized hybrid feed",
            description = "Returns a hybrid-ranked feed mixing social, collaborative, content-based, trending, and freshness signals")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feed returned"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<FeedResponse> getPersonalizedFeed(
            @PathVariable @NotBlank @Size(max = 50) String userId,
            @RequestParam(defaultValue = "0")  @Min(0)           int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        log.info("Personalized feed request | user={} page={} size={}", userId, page, size);
        long t0 = System.currentTimeMillis();
        FeedResponse response = feedRankingService.getPersonalizedFeed(userId, page, size);
        log.info("Personalized feed | user={} posts={} durationMs={}",
                userId, response.getPosts().size(), System.currentTimeMillis() - t0);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Following-only chronological feed
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get chronological following feed",
            description = "Returns posts from followed users, sorted by creation date, newest first")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feed returned")
    })
    @GetMapping("/{userId}/following")
    public ResponseEntity<FeedResponse> getFollowingFeed(
            @PathVariable @NotBlank @Size(max = 50) String userId,
            @RequestParam(defaultValue = "0")  @Min(0)           int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        log.info("Following feed request | user={} page={} size={}", userId, page, size);
        return ResponseEntity.ok(feedRankingService.getFollowingFeed(userId, page, size));
    }

    // -------------------------------------------------------------------------
    // Global trending
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get global trending posts",
            description = "Returns top posts by interaction count in the last 72 hours")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trending posts returned")
    })
    @GetMapping("/trending")
    public ResponseEntity<List<PostDoc>> getTrendingPosts(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {

        log.info("Trending posts request | limit={}", limit);
        return ResponseEntity.ok(feedRankingService.getTrendingPosts(limit));
    }

    // -------------------------------------------------------------------------
    // Debug / comparison endpoints
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Collaborative-only recommendations (debug)",
            description = "Raw collaborative-filtering signal — useful for A/B comparison")
    @ApiResponse(responseCode = "200", description = "Posts returned")
    @GetMapping("/{userId}/collaborative")
    public ResponseEntity<List<PostDoc>> getCollaborative(
            @PathVariable @NotBlank @Size(max = 50) String userId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {

        return ResponseEntity.ok(feedRankingService.getCollaborativeRecommendations(userId, limit));
    }

    @Operation(
            summary = "Content-based recommendations (debug)",
            description = "Raw content-based signal based on the user's category preferences")
    @ApiResponse(responseCode = "200", description = "Posts returned")
    @GetMapping("/{userId}/content-based")
    public ResponseEntity<List<PostDoc>> getContentBased(
            @PathVariable @NotBlank @Size(max = 50) String userId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {

        return ResponseEntity.ok(feedRankingService.getContentBasedRecommendations(userId, limit));
    }
}
