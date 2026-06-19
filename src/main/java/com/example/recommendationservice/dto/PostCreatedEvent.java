package com.example.recommendationservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
public class PostCreatedEvent {
    private String postId;
    private String authorId;
    private String text;
    private List<String> tags;
    private String category;
    private String postType;
    private Instant createdAt;
}