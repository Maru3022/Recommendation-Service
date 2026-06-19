package com.example.recommendationservice.service;

import com.example.recommendationservice.model.PostAction;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.repository.PostActionRepository;
import com.example.recommendationservice.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tracks user interactions with posts.
 * <p>
 * On LIKE/SAVE the post's category and tags are reflected into Redis
 * preference structures, enabling content-based recommendations.
 * On LIKE, the post embedding is used to update the user's interest embedding
 * (exponential moving average of last N liked posts).
 * </p>
 *
 * <h3>Action weights used for preference scoring:</h3>
 * <ul>
 *   <li>VIEW  → 1</li>
 *   <li>LIKE  → 3</li>
 *   <li>SAVE  → 4</li>
 *   <li>COMMENT/SHARE → 2</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostActionService {

    private static final int INTEREST_EMBEDDING_ALPHA_PERCENT = 30; // 0.3 EMA weight for new post

    private final PostActionRepository postActionRepository;
    private final PostSearchRepository postSearchRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public PostAction trackAction(String userId, String postId, String actionType) {
        PostAction.ActionType actionTypeEnum = PostAction.ActionType.valueOf(actionType.toUpperCase());
        PostAction action = PostAction.builder()
                .postId(postId)
                .userId(userId)
                .actionType(actionTypeEnum)
                .createdAt(Instant.now())
                .build();

        postActionRepository.save(action);
        log.debug("Tracked action {} for user {} on post {}", actionType, userId, postId);

        updateUserPreferences(postId, userId, actionType);
        return action;
    }

    public List<PostAction> getUserActionHistory(String userId) {
        return postActionRepository.findByUserId(userId);
    }

    public List<PostAction> getActionsByPostId(String postId) {
        return postActionRepository.findByPostId(postId);
    }

    public Optional<PostAction> getActionById(String id) {
        try {
            return postActionRepository.findById(Long.parseLong(id));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void updateUserPreferences(String postId, String userId, String actionType) {
        PostAction.ActionType actionTypeEnum = PostAction.ActionType.valueOf(actionType.toUpperCase());
        int weight = actionTypeEnum == PostAction.ActionType.VIEW ? 1 
                : actionTypeEnum == PostAction.ActionType.LIKE ? 3
                : actionTypeEnum == PostAction.ActionType.SAVE ? 4
                : actionTypeEnum == PostAction.ActionType.COMMENT || actionTypeEnum == PostAction.ActionType.SHARE ? 2
                : 0;
        if (weight == 0) return;

        try {
            Optional<PostDoc> postOpt = postSearchRepository.findById(postId);
            if (postOpt.isEmpty()) {
                log.debug("Post {} not found in ES, skipping preference update", postId);
                return;
            }
            PostDoc post = postOpt.get();

            // --- category_preferences hash: category → cumulative score ---
            if (post.getCategory() != null && !post.getCategory().isBlank()) {
                String prefKey = "user:" + userId + ":category_preferences";
                redisTemplate.opsForHash().increment(prefKey, post.getCategory(), weight);
                updateFavoriteCategory(userId, prefKey);
            }

            // --- per-tag preferences (same hash, prefixed with "tag:") ---
            if (post.getTags() != null) {
                String prefKey = "user:" + userId + ":category_preferences";
                for (String tag : post.getTags()) {
                    redisTemplate.opsForHash().increment(prefKey, "tag:" + tag, weight);
                }
            }

            // --- interest_embedding (EMA over liked/saved post embeddings) ---
            if (("LIKE".equalsIgnoreCase(actionType) || "SAVE".equalsIgnoreCase(actionType))
                    && post.getEmbedding() != null) {
                updateInterestEmbedding(userId, post.getEmbedding());
            }

        } catch (Exception e) {
            log.warn("Failed to update preferences for user {} on post {}: {}", userId, postId, e.getMessage());
        }
    }

    private void updateFavoriteCategory(String userId, String prefKey) {
        try {
            Object topCategory = redisTemplate.opsForHash().entries(prefKey).entrySet().stream()
                    .filter(e -> !e.getKey().toString().startsWith("tag:"))
                    .max((e1, e2) -> {
                        long v1 = toLong(e1.getValue());
                        long v2 = toLong(e2.getValue());
                        return Long.compare(v1, v2);
                    })
                    .map(e -> e.getKey())
                    .orElse(null);

            if (topCategory != null) {
                redisTemplate.opsForValue().set("user:" + userId + ":fav_category", topCategory.toString());
                log.debug("Updated fav_category for user {}: {}", userId, topCategory);
            }
        } catch (Exception e) {
            log.warn("Failed to update fav_category for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Exponential moving average: new = alpha * postEmb + (1-alpha) * existing
     * Stored as a serialised float[] under "user:{userId}:interest_embedding".
     */
    private void updateInterestEmbedding(String userId, float[] postEmbedding) {
        try {
            String key = "user:" + userId + ":interest_embedding";
            Object existing = redisTemplate.opsForValue().get(key);

            float alpha = INTEREST_EMBEDDING_ALPHA_PERCENT / 100.0f;
            float[] updated;

            if (existing instanceof float[] existingEmb && existingEmb.length == postEmbedding.length) {
                updated = new float[postEmbedding.length];
                for (int i = 0; i < postEmbedding.length; i++) {
                    updated[i] = alpha * postEmbedding[i] + (1 - alpha) * existingEmb[i];
                }
            } else {
                updated = postEmbedding.clone();
            }

            redisTemplate.opsForValue().set(key, updated);
            log.debug("Updated interest embedding for user {}", userId);
        } catch (Exception e) {
            log.warn("Failed to update interest embedding for user {}: {}", userId, e.getMessage());
        }
    }

    private int actionWeight(String actionType) {
        if (actionType == null) return 0;
        return switch (actionType.toUpperCase()) {
            case "VIEW"    -> 1;
            case "COMMENT", "SHARE" -> 2;
            case "LIKE"    -> 3;
            case "SAVE"    -> 4;
            default        -> 0;   // HIDE, REPORT — no positive signal
        };
    }

    private long toLong(Object value) {
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return 0L;
    }
}
