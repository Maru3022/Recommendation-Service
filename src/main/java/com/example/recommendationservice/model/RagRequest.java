package com.example.recommendationservice.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RagRequest {

    @NotBlank(message = "Query cannot be blank")
    private String query;

    @NotBlank(message = "User ID cannot be blank")
    private String userId;
}
