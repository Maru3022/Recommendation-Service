package com.example.recommendationservice.service;

import com.example.recommendationservice.model.UserProfile;
import com.example.recommendationservice.repository.UserProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;

    @Value("${recommendation.action.interest-embedding-alpha-percent:30}")
    private int alphaPercent;

    @Transactional
    public UserProfile getOrCreate(String userId) {
        return userProfileRepository.findById(userId).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setUserId(userId);
            profile.setUpdatedAt(Instant.now());
            return userProfileRepository.save(profile);
        });
    }

    @Transactional
    public void updateInterestEmbedding(String userId, float[] postEmbedding) {
        UserProfile profile = getOrCreate(userId);
        float[] current = deserializeEmbedding(profile.getInterestEmbeddingJson());
        float alpha = alphaPercent / 100.0f;

        float[] updated;
        if (current == null || current.length != postEmbedding.length) {
            updated = postEmbedding;
        } else {
            updated = new float[postEmbedding.length];
            for (int i = 0; i < postEmbedding.length; i++) {
                updated[i] = alpha * postEmbedding[i] + (1 - alpha) * current[i];
            }
        }

        profile.setInterestEmbeddingJson(serializeEmbedding(updated));
        profile.setUpdatedAt(Instant.now());
        userProfileRepository.save(profile);
    }

    @Transactional
    public void recordLike(String userId, String postId) {
        UserProfile profile = getOrCreate(userId);
        profile.getLikedPostIds().add(postId);
        userProfileRepository.save(profile);
    }

    @Transactional
    public void recordSave(String userId, String postId) {
        UserProfile profile = getOrCreate(userId);
        profile.getSavedPostIds().add(postId);
        userProfileRepository.save(profile);
    }

    @Transactional
    public void recordView(String userId, String postId) {
        UserProfile profile = getOrCreate(userId);
        profile.addViewedPost(postId);
        userProfileRepository.save(profile);
    }

    public Optional<float[]> getInterestEmbedding(String userId) {
        return userProfileRepository.findById(userId)
                .map(p -> deserializeEmbedding(p.getInterestEmbeddingJson()));
    }

    private float[] deserializeEmbedding(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<float[]>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize embedding: {}", e.getMessage());
            return null;
        }
    }

    private String serializeEmbedding(float[] embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception e) {
            log.warn("Failed to serialize embedding: {}", e.getMessage());
            return null;
        }
    }
}