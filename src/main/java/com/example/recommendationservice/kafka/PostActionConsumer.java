package com.example.recommendationservice.kafka;

import com.example.recommendationservice.dto.PostActionEvent;
import com.example.recommendationservice.model.PostAction;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.repository.PostActionRepository;
import com.example.recommendationservice.repository.PostSearchRepository;
import com.example.recommendationservice.service.FeedService;
import com.example.recommendationservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostActionConsumer {

    private final PostActionRepository postActionRepository;
    private final PostSearchRepository postSearchRepository;
    private final UserProfileService userProfileService;
    private final FeedService feedService;

    @KafkaListener(
            topics = "post-actions",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "postActionKafkaListenerContainerFactory"
    )
    public void consume(PostActionEvent event) {
        try {
            if (isIdempotentDuplicate(event)) {
                log.debug("Duplicate action skipped: user={} post={} type={}",
                        event.getUserId(), event.getPostId(), event.getActionType());
                return;
            }

            PostAction action = new PostAction();
            action.setUserId(event.getUserId());
            action.setPostId(event.getPostId());
            action.setActionType(event.getActionType());
            action.setWeight(event.getActionType().defaultWeight());
            action.setCreatedAt(event.getTimestamp() != null ? event.getTimestamp() : Instant.now());
            postActionRepository.save(action);

            updateUserProfile(event);
            feedService.invalidateCache(event.getUserId());
        } catch (Exception e) {
            log.error("Failed to process post-action event: {}", e.getMessage());
        }
    }

    private boolean isIdempotentDuplicate(PostActionEvent event) {
        return postActionRepository
                .findByUserIdAndPostIdAndActionType(event.getUserId(), event.getPostId(), event.getActionType())
                .isPresent();
    }

    private void updateUserProfile(PostActionEvent event) {
        switch (event.getActionType()) {
            case LIKE -> userProfileService.recordLike(event.getUserId(), event.getPostId());
            case SAVE -> userProfileService.recordSave(event.getUserId(), event.getPostId());
            case VIEW -> userProfileService.recordView(event.getUserId(), event.getPostId());
            default -> {}
        }

        boolean shouldUpdateEmbedding = switch (event.getActionType()) {
            case LIKE, SAVE, COMMENT -> true;
            default -> false;
        };

        if (shouldUpdateEmbedding) {
            Optional<PostDoc> postDoc = postSearchRepository.findById(event.getPostId());
            postDoc.ifPresent(doc -> {
                if (doc.getEmbedding() != null) {
                    userProfileService.updateInterestEmbedding(event.getUserId(), doc.getEmbedding());
                }
            });
        }
    }
}