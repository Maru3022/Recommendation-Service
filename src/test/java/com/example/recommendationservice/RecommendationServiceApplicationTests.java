package com.example.recommendationservice;

import com.example.recommendationservice.repository.PostActionRepository;
import com.example.recommendationservice.repository.PostSearchRepository;
import com.example.recommendationservice.repository.UserProfileRepository;
import com.example.recommendationservice.service.EmbeddingService;
import com.example.recommendationservice.service.FeedScoringService;
import com.example.recommendationservice.service.FeedService;
import com.example.recommendationservice.service.TrendingService;
import com.example.recommendationservice.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class RecommendationServiceApplicationTests {

    @MockitoBean PostSearchRepository postSearchRepository;
    @MockitoBean PostActionRepository postActionRepository;
    @MockitoBean UserProfileRepository userProfileRepository;
    @MockitoBean ElasticsearchOperations elasticsearchOperations;
    @MockitoBean EmbeddingService embeddingService;
    @MockitoBean FeedService feedService;
    @MockitoBean FeedScoringService feedScoringService;
    @MockitoBean TrendingService trendingService;
    @MockitoBean UserProfileService userProfileService;
    @MockitoBean RedisTemplate<String, Object> redisTemplate;

    @Test
    void contextLoads() {
    }
}