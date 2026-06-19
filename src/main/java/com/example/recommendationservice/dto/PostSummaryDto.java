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
    private String text;
    private String postType;
    private String category;
    private List<String> tags;
    private long likesCount;
    private long commentsCount;
    private double score;
    private Instant createdAt;
}