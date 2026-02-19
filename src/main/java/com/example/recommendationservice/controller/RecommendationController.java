package com.example.recommendationservice.controller;

import com.example.recommendationservice.model.RecommendationResponse;
import com.example.recommendationservice.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
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
        log.info("Received recommendation request | UserID: {} | Page: {} | Size: {}",userId,page,size);

        long startTime = System.currentTimeMillis();
        RecommendationResponse response = recommendationService.getRecommendations(userId,page,size);

        long duration = System.currentTimeMillis() - startTime;

        log.info("Sent recommendations to UserID: {} | Count: {} | Duration: {}ms", userId,response.getProducts().size(),duration);

        return ResponseEntity.ok(response);
    }
}