package com.example.recommendationservice.consumer;

import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Consumes events from other fitness-app services and auto-generates posts
 * so that users' workouts/achievements appear in the recommendation feed
 * without requiring explicit post creation.
 *
 * <h3>Supported topics (extend as needed):</h3>
 * <ul>
 *   <li>{@code training.created}  – a new Training was persisted (Training-Service)</li>
 *   <li>{@code train.completed}   – a Train (session) was completed (Trains-Service)</li>
 * </ul>
 *
 * <p>The payload contract intentionally uses {@link PostDoc} as a lightweight
 * DTO.  For richer event schemas the consumer can be adapted to accept a
 * dedicated event POJO and map it to a {@code PostDoc}.</p>
 *
 * <p><b>Extension point:</b> hook in {@code nutrition.logged} events from
 * Training-Nutrition to auto-generate MEAL_LOG posts.</p>
 */
@Service
@Profile("!replit")
@RequiredArgsConstructor
@Slf4j
public class TrainingEventConsumer {

    private final PostService postService;

    // -------------------------------------------------------------------------
    // Kafka listeners
    // -------------------------------------------------------------------------

    /**
     * Listens for training-completed events from Trains-Service.
     * Creates a WORKOUT_COMPLETED post in the feed index.
     */
    @KafkaListener(topics = "train.completed", groupId = "rec-group")
    public void onTrainCompleted(PostDoc event) {
        log.info("Received train.completed event for author {}", event.getAuthorId());
        try {
            PostDoc post = buildPost(event, "WORKOUT_COMPLETED",
                    List.of("workout", "completed"));
            postService.savePost(post);
            log.info("Auto-generated WORKOUT_COMPLETED post {} for author {}",
                    post.getId(), post.getAuthorId());
        } catch (Exception e) {
            log.error("Failed to process train.completed event: {}", e.getMessage(), e);
        }
    }

    /**
     * Listens for new training definitions created in Training-Service.
     * Creates a TIP post if the training has a description worth sharing.
     */
    @KafkaListener(topics = "training.created", groupId = "rec-group")
    public void onTrainingCreated(PostDoc event) {
        log.info("Received training.created event for author {}", event.getAuthorId());
        try {
            if (event.getText() == null || event.getText().isBlank()) {
                log.debug("training.created event has no text, skipping post generation");
                return;
            }
            PostDoc post = buildPost(event, "TIP", List.of("training", "tip"));
            postService.savePost(post);
            log.info("Auto-generated TIP post {} for author {}", post.getId(), post.getAuthorId());
        } catch (Exception e) {
            log.error("Failed to process training.created event: {}", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------

    private PostDoc buildPost(PostDoc event, String postType, List<String> defaultTags) {
        PostDoc post = new PostDoc();
        post.setAuthorId(event.getAuthorId());
        post.setAuthorDisplayName(event.getAuthorDisplayName());
        post.setAuthorAvatarUrl(event.getAuthorAvatarUrl());
        post.setText(event.getText() != null ? event.getText() : "");
        post.setMediaUrls(event.getMediaUrls());
        post.setPostType(postType);
        post.setCategory(event.getCategory());
        post.setRelatedTrainingId(event.getRelatedTrainingId());
        post.setDurationMinutes(event.getDurationMinutes());
        post.setCaloriesBurned(event.getCaloriesBurned());
        post.setVisibility(event.getVisibility() != null ? event.getVisibility() : "PUBLIC");
        post.setCreatedAt(event.getCreatedAt() != null ? event.getCreatedAt() : Instant.now());

        // Merge incoming tags with default tags, deduplicated
        List<String> tags = new java.util.ArrayList<>(defaultTags);
        if (event.getTags() != null) {
            event.getTags().stream()
                    .filter(t -> !tags.contains(t))
                    .forEach(tags::add);
        }
        post.setTags(tags);
        return post;
    }
}
