package com.example.recommendationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for semantic (RAG) post search.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchResponse {

    private String aiExplanation;
    private List<PostDoc> posts;
}
