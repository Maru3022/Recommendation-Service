package com.example.recommendationservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "recommendation.feed")
public class FeedProperties {

    private Weights weights = new Weights();
    private int trendingWindowHours = 72;
    private int freshnessHalfLifeHours = 48;
    private int maxAuthorPerPage = 2;
    private int candidateMultiplier = 3;
    private int maxCandidates = 300;
    private int similarUsersLimit = 5;
    private int knnCandidatesMultiplier = 10;
    private int knnMinCandidates = 50;

    @Data
    public static class Weights {
        private double social = 0.35;
        private double collaborative = 0.25;
        private double content = 0.20;
        private double trending = 0.15;
        private double freshness = 0.05;
    }
}