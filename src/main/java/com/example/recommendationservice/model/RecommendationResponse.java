package com.example.recommendationservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationResponse {

    private List<ProductDoc> products;
    private int currentPage;
    private long totalElements;
    private boolean hasNext;
}
