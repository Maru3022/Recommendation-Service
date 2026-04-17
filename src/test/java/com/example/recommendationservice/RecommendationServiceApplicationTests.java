package com.example.recommendationservice;

import com.example.recommendationservice.repository.ActionRepository;
import com.example.recommendationservice.repository.ProductSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class RecommendationServiceApplicationTests {

    // Подменяем Elasticsearch-репозитории, чтобы контекст поднимался без живого ES
    @MockitoBean
    private ProductSearchRepository productSearchRepository;

    @MockitoBean
    private ActionRepository actionRepository;

    @Test
    void contextLoads() {
    }

}
