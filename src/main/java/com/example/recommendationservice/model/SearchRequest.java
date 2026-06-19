package com.example.recommendationservice.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for semantic (RAG) post search.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequest {

    @NotBlank(message = "Query cannot be blank")
    @Size(max = 500, message = "Query cannot exceed 500 characters")
    private String query;

    @NotBlank(message = "User ID cannot be blank")
    private String userId;
}
