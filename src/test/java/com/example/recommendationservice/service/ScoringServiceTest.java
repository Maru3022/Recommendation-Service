package com.example.recommendationservice.service;

import com.example.recommendationservice.config.FeedProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock private FeedProperties feedProperties;

    @InjectMocks
    private ScoringService scoringService;

    private FeedProperties.Weights weights;

    @BeforeEach
    void setUp() {
        weights = new FeedProperties.Weights();
        weights.setSocial(0.35);
        weights.setCollaborative(0.25);
        weights.setContent(0.20);
        weights.setTrending(0.15);
        weights.setFreshness(0.05);
    }

    @Test
    void calculateFeedScore_combinesAllSignals() {
        when(feedProperties.getWeights()).thenReturn(weights);
        when(feedProperties.getFreshnessHalfLifeHours()).thenReturn(48);

        double score = scoringService.calculateFeedScore(1.0, 0.5, 0.8, 0.6, Instant.now());

        assertThat(score).isGreaterThan(0);
        assertThat(score).isLessThan(1.0);
    }

    @Test
    void calculateFeedScore_zeroSignalsStillHasFreshness() {
        when(feedProperties.getWeights()).thenReturn(weights);
        when(feedProperties.getFreshnessHalfLifeHours()).thenReturn(48);

        double score = scoringService.calculateFeedScore(0, 0, 0, 0, Instant.now());

        assertThat(score).isGreaterThan(0);
    }

    @Test
    void calculateEngagementScore_normalizesEngagement() {
        double score = scoringService.calculateEngagementScore(100, 20, 10, 5);

        assertThat(score).isGreaterThan(0);
        assertThat(score).isLessThanOrEqualTo(1.0);
    }

    @Test
    void calculateEngagementScore_handlesZeroEngagement() {
        assertThat(scoringService.calculateEngagementScore(0, 0, 0, 0)).isEqualTo(0.0);
    }

    @Test
    void calculateEngagementScore_handlesHighEngagement() {
        assertThat(scoringService.calculateEngagementScore(10000, 5000, 2000, 1000)).isEqualTo(1.0);
    }

    @Test
    void freshnessScore_recentPostReturnsHighScore() {
        when(feedProperties.getFreshnessHalfLifeHours()).thenReturn(48);

        assertThat(scoringService.freshnessScore(Instant.now())).isGreaterThan(0.9);
    }

    @Test
    void freshnessScore_oldPostReturnsLowScore() {
        when(feedProperties.getFreshnessHalfLifeHours()).thenReturn(48);

        assertThat(scoringService.freshnessScore(Instant.now().minusSeconds(86400 * 10))).isLessThan(0.5);
    }

    @Test
    void freshnessScore_nullCreatedAtReturnsZero() {
        assertThat(scoringService.freshnessScore(null)).isEqualTo(0.0);
    }
}
