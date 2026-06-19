package com.example.recommendationservice.service;

import com.example.recommendationservice.config.FeedProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ScoringService {

    private final FeedProperties feedProperties;

    public double calculateFeedScore(double social, double collaborative, 
                                     double content, double trending, Instant createdAt) {
        FeedProperties.Weights w = feedProperties.getWeights();
        double freshness = freshnessScore(createdAt);
        
        return w.getSocial() * social
                + w.getCollaborative() * collaborative
                + w.getContent() * content
                + w.getTrending() * trending
                + w.getFreshness() * freshness;
    }

    public double calculateEngagementScore(long likesCount, long commentsCount, 
                                          long savesCount, long sharesCount) {
        double raw = likesCount
                + 2.0 * commentsCount
                + 3.0 * savesCount
                + sharesCount;
        return Math.min(raw / 1000.0, 1.0);
    }

    public double freshnessScore(Instant createdAt) {
        if (createdAt == null) return 0.0;
        long hoursOld = Duration.between(createdAt, Instant.now()).toHours();
        double halfLife = feedProperties.getFreshnessHalfLifeHours();
        return Math.pow(0.5, hoursOld / halfLife);
    }
}
