package com.example.recommendationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response for the personalized post feed.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedResponse {

    private List<RankedPost> posts;
    private int currentPage;
    private long totalElements;
    private boolean hasNext;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RankedPost {
        private PostDoc post;
        /** Final hybrid score (0..1), useful for debug/A-B testing. */
        private double score;
    }
}
