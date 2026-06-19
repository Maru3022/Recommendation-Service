package com.example.recommendationservice.service;

import com.example.recommendationservice.config.FeedProperties;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.model.UserProfile;
import com.example.recommendationservice.model.UserProfileDoc;
import com.example.recommendationservice.repository.PostSearchRepository;
import com.example.recommendationservice.repository.UserProfileRepository;
import com.example.recommendationservice.repository.UserProfileSearchRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborativeFilteringService {

    private static final String SIMILAR_USERS_KEY = "similar_users:";
    private static final int SIMILAR_USERS_TTL_HOURS = 6;

    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;
    private final UserProfileSearchRepository userProfileSearchRepository;
    private final PostSearchRepository postSearchRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final FeedProperties feedProperties;

    public List<PostDoc> getCollaborativePosts(String userId, int limit, Set<String> excludeIds) {
        UserProfile currentUser = userProfileRepository.findById(userId).orElse(null);
        if (currentUser == null) return Collections.emptyList();

        Set<String> viewedByUser = new java.util.HashSet<>(currentUser.getViewedPostIds());

        List<String> similarUserIds = getSimilarUsersFromCache(userId);
        if (similarUserIds == null) {
            similarUserIds = computeSimilarUsers(userId);
            cacheSimilarUsers(userId, similarUserIds);
        }

        if (similarUserIds.isEmpty()) return Collections.emptyList();

        Set<String> likedPostIds = new LinkedHashSet<>();
        for (String similarUserId : similarUserIds) {
            userProfileRepository.findById(similarUserId).ifPresent(profile ->
                    profile.getLikedPostIds().stream()
                            .filter(id -> !excludeIds.contains(id))
                            .filter(id -> !viewedByUser.contains(id))
                            .forEach(likedPostIds::add)
            );
        }

        if (likedPostIds.isEmpty()) return Collections.emptyList();

        return postSearchRepository.findByIdIn(new ArrayList<>(likedPostIds))
                .stream()
                .limit(limit)
                .toList();
    }

    @Scheduled(fixedDelay = 21_600_000)
    public void refreshAllSimilarUsers() {
        log.info("Refreshing similar users cache...");
        List<UserProfile> allProfiles = userProfileRepository.findAll();
        allProfiles.stream()
                .filter(p -> p.getInterestEmbeddingJson() != null)
                .forEach(p -> {
                    List<String> similar = computeSimilarUsers(p.getUserId());
                    cacheSimilarUsers(p.getUserId(), similar);
                });
        log.info("Similar users cache refreshed for {} users", allProfiles.size());
    }

    private List<String> computeSimilarUsers(String userId) {
        float[] userEmbedding = userProfileService.getInterestEmbedding(userId).orElse(null);
        if (userEmbedding == null) return Collections.emptyList();

        int k = feedProperties.getSimilarUsersLimit();
        int numCandidates = k * feedProperties.getKnnCandidatesMultiplier();

        try {
            List<UserProfileDoc> similarDocs = userProfileSearchRepository.findSimilarByKnn(
                    userEmbedding, k, Math.max(numCandidates, feedProperties.getKnnMinCandidates())
            );

            return similarDocs.stream()
                    .filter(doc -> !doc.getUserId().equals(userId))
                    .map(UserProfileDoc::getUserId)
                    .limit(k)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("kNN search failed for user {}, falling back to empty result: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getSimilarUsersFromCache(String userId) {
        try {
            Object raw = redisTemplate.opsForValue().get(SIMILAR_USERS_KEY + userId);
            if (raw == null) return null;
            String json = raw instanceof String s ? s : objectMapper.writeValueAsString(raw);
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to read similar users cache for {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private void cacheSimilarUsers(String userId, List<String> similarUserIds) {
        try {
            String json = objectMapper.writeValueAsString(similarUserIds);
            redisTemplate.opsForValue().set(
                    SIMILAR_USERS_KEY + userId, json, SIMILAR_USERS_TTL_HOURS, TimeUnit.HOURS
            );
        } catch (Exception e) {
            log.warn("Failed to cache similar users for {}: {}", userId, e.getMessage());
        }
    }
}