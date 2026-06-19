package com.example.recommendationservice.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for creating a new post via REST.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePostRequest {

    @NotBlank(message = "Author ID cannot be blank")
    private String authorId;

    @NotBlank(message = "Author display name cannot be blank")
    private String authorDisplayName;

    private String authorAvatarUrl;

    @NotBlank(message = "Post text cannot be blank")
    @Size(max = 2000, message = "Post text cannot exceed 2000 characters")
    private String text;

    private List<String> mediaUrls;

    /**
     * WORKOUT_COMPLETED | ACHIEVEMENT | PROGRESS_PHOTO | TIP | MEAL_LOG | FREEFORM
     */
    @NotBlank(message = "Post type cannot be blank")
    private String postType;

    private List<String> tags;

    private String category;

    private String relatedTrainingId;

    private Double durationMinutes;

    private Double caloriesBurned;

    /**
     * PUBLIC | FOLLOWERS_ONLY | PRIVATE
     */
    @NotBlank(message = "Visibility cannot be blank")
    private String visibility;
}
