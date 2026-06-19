package com.example.recommendationservice.service;

import com.example.recommendationservice.config.FeedProperties;
import com.example.recommendationservice.dto.FeedRequest;
import com.example.recommendationservice.dto.FeedResponse;
import com.example.recommendationservice.dto.PostSummaryDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private static final String FEED_CACHE_PREFIX = "feed:";
    private static final long FEED_TTL_MINUTES = 10;

    private final FeedScoringService feedScoringService;
    private final FeedProperties feedProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public FeedResponse getFeed(FeedRequest request) {
        String cacheKey = FEED_CACHE_PREFIX + request.getUserId() + ":page:" + request.getPage();

        List<PostSummaryDto> cached = tryGetFromCache(cacheKey);
        if (cached != null) {
            return buildResponse(cached, request);
        }

        int candidateCount = Math.min(
                request.getSize() * feedProperties.getCandidateMultiplier(),
                feedProperties.getMaxCandidates()
        );

        List<PostSummaryDto> all = feedScoringService.buildFeed(
                request.getUserId(),
                candidateCount,
                request.getExcludePostIds()
        );

        int from = request.getPage() * request.getSize();
        int to = Math.min(from + request.getSize(), all.size());
        List<PostSummaryDto> page = from >= all.size() ? List.of() : all.subList(from, to);

        tryPutToCache(cacheKey, all);

        return buildResponse(page, request);
    }

    public void invalidateCache(String userId) {
        try {
            var keys = redisTemplate.keys(FEED_CACHE_PREFIX + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Invalidated {} cache keys for user {}", keys.size(), userId);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate cache for user {}: {}", userId, e.getMessage());
        }
    }

    private List<PostSummaryDto> tryGetFromCache(String cacheKey) {
        try {
            Object raw = redisTemplate.opsForValue().get(cacheKey);
            if (raw == null) return null;
            String json = raw instanceof String s ? s : objectMapper.writeValueAsString(raw);
            return objectMapper.readValue(json, new TypeReference<List<PostSummaryDto>>() {});
        } catch (Exception e) {
            log.warn("Cache read failed for key {}: {}", cacheKey, e.getMessage());
            return null;
        }
    }

    private void tryPutToCache(String cacheKey, List<PostSummaryDto> posts) {
        try {
            String json = objectMapper.writeValueAsString(posts);
            redisTemplate.opsForValue().set(cacheKey, json, FEED_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Cache write failed for key {}: {}", cacheKey, e.getMessage());
        }
    }

    private FeedResponse buildResponse(List<PostSummaryDto> posts, FeedRequest request) {
        return FeedResponse.builder()
                .posts(posts)
                .page(request.getPage())
                .size(posts.size())
                .hasMore(posts.size() == request.getSize())
                .build();
    }
}