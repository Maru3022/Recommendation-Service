package com.example.recommendationservice.controller;

import com.example.recommendationservice.model.SearchRequest;
import com.example.recommendationservice.model.SearchResponse;
import com.example.recommendationservice.service.SemanticSearchService;
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
@RequestMapping("/api/feed")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
@Tag(name = "Semantic Search API", description = "Natural-language search over posts using kNN embeddings and LLM explanation")
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;

    @Operation(
            summary = "Semantic post search (RAG)",
            description = "Embeds the query, performs kNN search on post embeddings, and returns an AI-generated explanation of why the results are relevant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results with AI explanation"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        log.info("Semantic search | user={} query='{}'", request.getUserId(), request.getQuery());
        SearchResponse response = semanticSearchService.searchByQuery(
                request.getQuery(), request.getUserId(), 10);
        return ResponseEntity.ok(response);
    }
}
