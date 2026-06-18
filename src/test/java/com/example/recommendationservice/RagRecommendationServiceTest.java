package com.example.recommendationservice;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.model.RagResponse;
import com.example.recommendationservice.repository.ProductSearchRepository;
import com.example.recommendationservice.service.EmbeddingService;
import com.example.recommendationservice.service.RagRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RagRecommendationServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec chatClientRequestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @InjectMocks
    private RagRecommendationService ragRecommendationService;

    private List<ProductDoc> sampleProducts;
    private float[] sampleEmbedding;

    @BeforeEach
    void setUp() {
        sampleEmbedding = new float[1536];
        Arrays.fill(sampleEmbedding, 0.5f);

        sampleProducts = Arrays.asList(
                new ProductDoc("1", "Laptop", "High-performance laptop", "Electronics", 999.0, "url1", sampleEmbedding),
                new ProductDoc("2", "Book", "Programming book", "Books", 19.0, "url2", sampleEmbedding),
                new ProductDoc("3", "Shirt", "Cotton shirt", "Fashion", 49.0, "url3", sampleEmbedding)
        );

        lenient().when(chatClientBuilder.build()).thenReturn(chatClient);
        lenient().when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        lenient().when(chatClientRequestSpec.system(anyString())).thenReturn(chatClientRequestSpec);
        lenient().when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
        lenient().when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        lenient().when(callResponseSpec.content()).thenReturn("AI explanation");
    }

    @Test
    @Disabled("Requires Elasticsearch dependencies which are excluded in test profile")
    void searchByQuery_WithValidQueryAndKnnResults_ShouldReturnResponse() {
        // Given
        String query = "I need a laptop";
        String userId = "user1";
        when(embeddingService.generateEmbeddingForQuery(query)).thenReturn(Optional.of(sampleEmbedding));

        // Mock fallback search (since kNN is temporarily disabled)
        when(productSearchRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(sampleProducts.subList(0, 2)));

        // When
        RagResponse response = ragRecommendationService.searchByQuery(query, userId, 10);

        // Then
        assertNotNull(response);
        assertNotNull(response.getProducts());
        assertEquals(2, response.getProducts().size());
        assertEquals("AI explanation", response.getAiExplanation());
    }

    @Test
    @Disabled("Requires Elasticsearch dependencies which are excluded in test profile")
    void searchByQuery_WhenKnnFails_ShouldFallbackToSimpleSearch() {
        // Given
        String query = "I need a laptop";
        String userId = "user1";
        when(embeddingService.generateEmbeddingForQuery(query)).thenReturn(Optional.of(sampleEmbedding));

        // Mock fallback search
        when(productSearchRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(sampleProducts));

        // When
        RagResponse response = ragRecommendationService.searchByQuery(query, userId, 10);

        // Then
        assertNotNull(response);
        assertNotNull(response.getProducts());
        assertEquals(3, response.getProducts().size());
    }

    @Test
    void searchByQuery_WhenEmbeddingFails_ShouldFallback() {
        // Given
        String query = "I need a laptop";
        String userId = "user1";
        when(embeddingService.generateEmbeddingForQuery(query)).thenReturn(Optional.empty());

        // Mock fallback search
        when(productSearchRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(sampleProducts));

        // When
        RagResponse response = ragRecommendationService.searchByQuery(query, userId, 10);

        // Then
        assertNotNull(response);
        assertNotNull(response.getProducts());
    }
}
