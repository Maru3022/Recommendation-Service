package com.example.recommendationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSummaryDto {
    private String postId;
    private String authorId;
    private String authorDisplayName;
    private String authorAvatarUrl;
    private String text;
    private String postType;
    private String category;
    private List<String> tags;
    private List<String> mediaUrls;
    private String relatedTrainingId;
    private Double durationMinutes;
    private Double caloriesBurned;
    private long likesCount;
    private long commentsCount;
    private long sharesCount;
    private long savesCount;
    private double score;
    private Instant createdAt;
}
