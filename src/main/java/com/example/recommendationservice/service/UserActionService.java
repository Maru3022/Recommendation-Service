package com.example.recommendationservice.service;

import com.example.recommendationservice.model.UserAction;
import com.example.recommendationservice.repository.ActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActionService {

    private final ActionRepository actionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public void trackAction(UserAction userAction) {
        try {
            // Generate ID if not present
            if (userAction.getId() == null || userAction.getId().isEmpty()) {
                userAction.setId(UUID.randomUUID().toString());
            }

            // Save to Elasticsearch
            actionRepository.save(userAction);

            // Update user preferences in Redis based on action type
            updateUserPreferences(userAction);

            log.debug("Successfully tracked action: {}", userAction.getId());
        } catch (Exception e) {
            log.error("Failed to track user action: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to track user action", e);
        }
    }

    public Iterable<UserAction> getUserActionHistory(String userId) {
        try {
            return actionRepository.findByUserId(userId);
        } catch (Exception e) {
            log.error("Failed to fetch user action history for userId: {}", userId, e);
            throw new RuntimeException("Failed to fetch user action history", e);
        }
    }

    private void updateUserPreferences(UserAction userAction) {
        try {
            // For view or like actions, update category preferences
            if ("view".equals(userAction.getActionType()) || "like".equals(userAction.getActionType())) {
                // This would typically involve fetching the product to get its category
                // For now, we'll use a simple approach
                String preferenceKey = "user:" + userAction.getUserId() + ":category_preferences";
                
                // Increment category score (simplified - in real implementation, fetch product category)
                redisTemplate.opsForHash().increment(preferenceKey, "general", 1);
                
                // Set favorite category based on most interactions
                updateFavoriteCategory(userAction.getUserId());
            }
        } catch (Exception e) {
            log.warn("Failed to update user preferences for userId: {}", userAction.getUserId(), e);
            // Don't throw here as tracking is more important than preference updates
        }
    }

    private void updateFavoriteCategory(String userId) {
        try {
            String preferenceKey = "user:" + userId + ":category_preferences";
            Object topCategory = redisTemplate.opsForHash().entries(preferenceKey).entrySet().stream()
                    .max((e1, e2) -> Long.compare((Long) e2.getValue(), (Long) e1.getValue()))
                    .map(entry -> entry.getKey())
                    .orElse(null);

            if (topCategory != null) {
                String favCategoryKey = "user:" + userId + ":fav_category";
                redisTemplate.opsForValue().set(favCategoryKey, topCategory.toString());
                log.debug("Updated favorite category for user {}: {}", userId, topCategory);
            }
        } catch (Exception e) {
            log.warn("Failed to update favorite category for userId: {}", userId, e);
        }
    }
}
