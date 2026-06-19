package com.example.recommendationservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class FeedRequest {

    @NotBlank
    private String userId;

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(50)
    private int size = 20;

    private Set<String> excludePostIds = new HashSet<>();
}