package com.example.recommendationservice.controller;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.model.RecommendationResponse;
import com.example.recommendationservice.service.RecommendationService;
import com.example.recommendationservice.service.EnhancedRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/recommendations")
@Validated
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "https://recommendation.example.com"})
@Tag(name = "Recommendation API", description = "API for getting product recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final EnhancedRecommendationService enhancedRecommendationService;

    @Operation(summary = "Get recommendations for a user", description = "Returns a list of recommended products for a given user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendations found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<RecommendationResponse> getRecommendations(
            @PathVariable @NotBlank @Size(max = 50) String userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ){
        log.info("Received recommendation request | UserID: {} | Page: {} | Size: {}",userId,page,size);

        long startTime = System.currentTimeMillis();
        RecommendationResponse response = recommendationService.getRecommendations(userId,page,size);

        long duration = System.currentTimeMillis() - startTime;

        log.info("Sent recommendations to UserID: {} | Count: {} | Duration: {}ms", userId,response.getProducts().size(),duration);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get popular products", description = "Returns a list of popular products across all users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Popular products found")
    })
    @GetMapping("/popular")
    public ResponseEntity<List<ProductDoc>> getPopular(@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit){
        return ResponseEntity.ok(recommendationService.getPopularProducts(limit));
    }

    @Operation(summary = "Get collaborative recommendations", description = "Returns recommendations based on similar users' behavior")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendations found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}/collaborative")
    public ResponseEntity<List<ProductDoc>> getCollaborativeRecommendations(
            @PathVariable @NotBlank @Size(max = 50) String userId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit
    ){
        log.info("Getting collaborative recommendations for userId: {}", userId);
        List<ProductDoc> recommendations = enhancedRecommendationService.getCollaborativeRecommendations(userId, limit);
        return ResponseEntity.ok(recommendations);
    }

    @Operation(summary = "Get content-based recommendations", description = "Returns recommendations based on user's category preferences")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendations found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}/content-based")
    public ResponseEntity<List<ProductDoc>> getContentBasedRecommendations(
            @PathVariable @NotBlank @Size(max = 50) String userId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit
    ){
        log.info("Getting content-based recommendations for userId: {}", userId);
        List<ProductDoc> recommendations = enhancedRecommendationService.getContentBasedRecommendations(userId, limit);
        return ResponseEntity.ok(recommendations);
    }

    @Operation(summary = "Get trending products", description = "Returns currently trending products based on recent user activity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trending products found")
    })
    @GetMapping("/trending")
    public ResponseEntity<List<ProductDoc>> getTrendingProducts(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit
    ){
        log.info("Getting trending products");
        List<ProductDoc> trending = enhancedRecommendationService.getTrendingProducts(limit);
        return ResponseEntity.ok(trending);
    }
}
