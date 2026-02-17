package com.example.recommendationservice.controller;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.model.RecommendationResponse;
import com.example.recommendationservice.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{userId}")
    public ResponseEntity<RecommendationResponse> getRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        return ResponseEntity.ok(recommendationService.getRecommendations(userId, page, size));
    }
}