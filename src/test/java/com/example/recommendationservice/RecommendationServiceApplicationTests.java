package com.example.recommendationservice;

import com.example.recommendationservice.repository.PostActionRepository;
import com.example.recommendationservice.repository.PostSearchRepository;
import com.example.recommendationservice.service.EmbeddingService;
import com.example.recommendationservice.service.FeedRankingService;
import com.example.recommendationservice.service.PostActionService;
import com.example.recommendationservice.service.PostService;
import com.example.recommendationservice.service.SemanticSearchService;
import com.example.recommendationservice.service.SocialGraphService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Smoke test — verifies the Spring application context loads correctly
 * without requiring live Elasticsearch, Redis, or Kafka.
 *
 * All infrastructure beans are mocked via @MockitoBean.
 */
@SpringBootTest
@ActiveProfiles("test")
class RecommendationServiceApplicationTests {

    // --- Elasticsearch ---
    @MockitoBean PostSearchRepository  postSearchRepository;
    @MockitoBean PostActionRepository  postActionRepository;
    @MockitoBean ElasticsearchOperations elasticsearchOperations;

    // --- Services ---
    @MockitoBean EmbeddingService      embeddingService;
    @MockitoBean PostService           postService;
    @MockitoBean PostActionService     postActionService;
    @MockitoBean FeedRankingService    feedRankingService;
    @MockitoBean SemanticSearchService semanticSearchService;
    @MockitoBean SocialGraphService    socialGraphService;

    @Test
    void contextLoads() {
        // If the context starts without exceptions, the test passes.
    }
}
