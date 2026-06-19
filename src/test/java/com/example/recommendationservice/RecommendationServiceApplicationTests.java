package com.example.recommendationservice;

import com.example.recommendationservice.repository.PostActionRepository;
import com.example.recommendationservice.repository.PostSearchRepository;
import com.example.recommendationservice.repository.UserProfileRepository;
import com.example.recommendationservice.repository.UserProfileSearchRepository;
import com.example.recommendationservice.service.EmbeddingService;
import com.example.recommendationservice.service.FeedScoringService;
import com.example.recommendationservice.service.FeedService;
import com.example.recommendationservice.service.TrendingService;
import com.example.recommendationservice.service.UserProfileService;
import com.example.recommendationservice.service.CollaborativeFilteringService;
import com.example.recommendationservice.service.ContentBasedService;
import com.example.recommendationservice.service.SemanticSearchService;
import com.example.recommendationservice.service.FeedRankingService;
import com.example.recommendationservice.service.SocialSignalService;
import com.example.recommendationservice.service.PostService;
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
    @MockitoBean UserProfileSearchRepository userProfileSearchRepository;
    @MockitoBean ElasticsearchOperations elasticsearchOperations;
    @MockitoBean EmbeddingService embeddingService;
    @MockitoBean FeedService feedService;
    @MockitoBean FeedScoringService feedScoringService;
    @MockitoBean FeedRankingService feedRankingService;
    @MockitoBean TrendingService trendingService;
    @MockitoBean UserProfileService userProfileService;
    @MockitoBean CollaborativeFilteringService collaborativeFilteringService;
    @MockitoBean ContentBasedService contentBasedService;
    @MockitoBean SemanticSearchService semanticSearchService;
    @MockitoBean SocialSignalService socialSignalService;
    @MockitoBean PostService postService;
    @MockitoBean RedisTemplate<String, Object> redisTemplate;

    @Test
    void contextLoads() {
    }
}