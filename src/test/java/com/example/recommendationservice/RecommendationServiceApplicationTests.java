package com.example.recommendationservice;

import com.example.recommendationservice.repository.ActionRepository;
import com.example.recommendationservice.repository.ProductSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RecommendationServiceApplicationTests {

    // Подменяем Elasticsearch-репозитории, чтобы контекст поднимался без живого ES
    @MockBean
    private ProductSearchRepository productSearchRepository;

    @MockBean
    private ActionRepository actionRepository;

    @Test
    void contextLoads() {
    }

}
