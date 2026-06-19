package com.example.recommendationservice.service;

import com.example.recommendationservice.config.FeedProperties;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.repository.PostActionRepository;
import com.example.recommendationservice.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendingService {

    private static final String TRENDING_KEY = "trending:posts";

    private final PostActionRepository postActionRepository;
    private final PostSearchRepository postSearchRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final FeedProperties feedProperties;

    public List<PostDoc> getTrendingPosts(int limit) {
        Set<Object> cached = redisTemplate.opsForZSet().reverseRange(TRENDING_KEY, 0, limit - 1);
        if (cached != null && !cached.isEmpty()) {
            List<String> ids = cached.stream().map(Object::toString).toList();
            return postSearchRepository.findByIdIn(ids);
        }
        return refreshAndReturn(limit);
    }

    @Scheduled(fixedDelay = 3600000)
    public void refreshTrending() {
        refreshAndReturn(100);
    }

    private List<PostDoc> refreshAndReturn(int limit) {
        Instant since = Instant.now().minus(Duration.ofHours(feedProperties.getTrendingWindowHours()));
        List<Object[]> rows = postActionRepository.findTrendingPostIds(since, PageRequest.of(0, 200));

        redisTemplate.delete(TRENDING_KEY);

        List<String> postIds = new ArrayList<>();
        for (Object[] row : rows) {
            String postId = row[0].toString();
            long count = ((Number) row[1]).longValue();
            redisTemplate.opsForZSet().add(TRENDING_KEY, postId, count);
            postIds.add(postId);
        }
        redisTemplate.expire(TRENDING_KEY, 1, TimeUnit.HOURS);

        List<String> topIds = postIds.stream().limit(limit).toList();
        return postSearchRepository.findByIdIn(topIds);
    }
}