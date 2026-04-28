package com.example.recommendationservice;

import com.example.recommendationservice.repository.ActionRepository;
import com.example.recommendationservice.repository.ProductSearchRepository;
import com.example.recommendationservice.service.EnhancedRecommendationService;
import com.example.recommendationservice.service.RecommendationService;
import com.example.recommendationservice.service.UserActionService;
import com.example.recommendationservice.service.ProductSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class RecommendationServiceApplicationTests {

    // Подменяем репозитории, чтобы контекст поднимался без живого Elasticsearch
    @MockitoBean
    private ProductSearchRepository productSearchRepository;

    @MockitoBean
    private ActionRepository actionRepository;

    // Подменяем сервисы, чтобы не требовались реальные Redis, Kafka и другие зависимости
    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private EnhancedRecommendationService enhancedRecommendationService;

    @MockitoBean
    private UserActionService userActionService;

    @MockitoBean
    private ProductSyncService productSyncService;

    @Test
    void contextLoads() {
    }

}
