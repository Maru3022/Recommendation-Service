package com.example.recommendationservice.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for tracking a user action on a post.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackActionRequest {

    @NotBlank(message = "User ID cannot be blank")
    private String userId;

    /**
     * VIEW | LIKE | COMMENT | SHARE | SAVE | HIDE | REPORT
     */
    @NotBlank(message = "Action type cannot be blank")
    private String actionType;
}
