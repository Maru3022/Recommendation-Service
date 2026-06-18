package com.example.recommendationservice.controller;

import com.example.recommendationservice.model.RagRequest;
import com.example.recommendationservice.model.RagResponse;
import com.example.recommendationservice.service.RagRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "https://recommendation.example.com"})
@Tag(name = "RAG Recommendation API", description = "API for RAG-based product recommendations using natural language queries")
public class RagController {

    private final RagRecommendationService ragRecommendationService;

    @Operation(summary = "Search products using natural language query with RAG",
            description = "Performs kNN search using embeddings, then uses LLM to explain recommendations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendations found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/rag")
    public ResponseEntity<RagResponse> searchWithRag(@Valid @RequestBody RagRequest request) {
        log.info("Received RAG search request for user: {}, query: {}", request.getUserId(), request.getQuery());
        RagResponse response = ragRecommendationService.searchByQuery(request.getQuery(), request.getUserId(), 10);
        return ResponseEntity.ok(response);
    }
}
