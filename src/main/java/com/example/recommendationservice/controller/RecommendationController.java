package com.example.recommendationservice.controller;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<ProductDoc>> getRecommendations(
            @PathVariable String userId
    ){
        return ResponseEntity.ok(recommendationService.getRecommendations(userId));
    }
}