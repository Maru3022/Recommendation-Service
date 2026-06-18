package com.example.recommendationservice;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.model.UserAction;
import com.example.recommendationservice.repository.ActionRepository;
import com.example.recommendationservice.repository.ProductSearchRepository;
import com.example.recommendationservice.service.EnhancedRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EnhancedRecommendationServiceTest {

    @Mock
    private ProductSearchRepository productRepository;

    @Mock
    private ActionRepository actionRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private org.springframework.data.redis.core.HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private EnhancedRecommendationService enhancedRecommendationService;

    private List<ProductDoc> sampleProducts;

    @BeforeEach
    void setUp() {
        float[] sampleEmbedding = new float[1536];
        sampleProducts = Arrays.asList(
            new ProductDoc("1", "Laptop", "High-performance laptop", "Electronics", 999.0, "url1", sampleEmbedding),
            new ProductDoc("2", "Book", "Programming book", "Books", 19.0, "url2", sampleEmbedding),
            new ProductDoc("3", "Shirt", "Cotton shirt", "Fashion", 49.0, "url3", sampleEmbedding)
        );
        
        // Setup RedisTemplate mock to return HashOperations
        org.mockito.Mockito.lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void getCollaborativeRecommendations_WithSimilarUsers_ShouldReturnRecommendations() {
        // Given
        String userId = "user1";
        when(actionRepository.findByUserId(userId)).thenReturn(Arrays.asList(
            new UserAction("a1", userId, "p1", "view")
        ));

        when(actionRepository.findByUserId("user2")).thenReturn(Arrays.asList(
            new UserAction("a2", "user2", "p2", "like"),
            new UserAction("a3", "user2", "p3", "view")
        ));

        when(actionRepository.findByProductId("p1")).thenReturn(Arrays.asList(
            new UserAction("a1", userId, "p1", "view"),
            new UserAction("a2", "user2", "p1", "view")
        ));

        when(productRepository.findAllByIdIn(any())).thenReturn(sampleProducts);

        // When
        List<ProductDoc> result = enhancedRecommendationService.getCollaborativeRecommendations(userId, 10);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void getContentBasedRecommendations_WithPreferences_ShouldReturnRecommendations() {
        // Given
        String userId = "user1";
        java.util.Map<Object, Object> preferences = java.util.Map.of("Electronics", 5L, "Books", 3L);
        when(hashOperations.entries(anyString())).thenReturn(preferences);
        
        when(productRepository.findByCategory(eq("Electronics"), any())).thenReturn(
            new org.springframework.data.domain.PageImpl<>(Arrays.asList(sampleProducts.get(0)))
        );

        // When
        List<ProductDoc> result = enhancedRecommendationService.getContentBasedRecommendations(userId, 5);

        // Then
        assertNotNull(result);
        verify(hashOperations).entries(anyString());
        verify(productRepository).findByCategory(eq("Electronics"), any());
    }

    @Test
    void getTrendingProducts_WithData_ShouldReturnTrending() {
        // Given
        List<UserAction> actions = Arrays.asList(
            new UserAction("a1", "user1", "p1", "view"),
            new UserAction("a2", "user2", "p1", "view"),
            new UserAction("a3", "user3", "p2", "like")
        );
        when(actionRepository.findAll()).thenReturn(actions);
        when(productRepository.findAllByIdIn(any())).thenReturn(sampleProducts);

        // When
        List<ProductDoc> result = enhancedRecommendationService.getTrendingProducts(10);

        // Then
        assertNotNull(result);
        verify(actionRepository).findAll();
    }

    @Test
    void getCollaborativeRecommendations_WithNoSimilarUsers_ShouldReturnEmpty() {
        // Given
        String userId = "user1";
        when(actionRepository.findByUserId(userId)).thenReturn(Arrays.asList(
            new UserAction("a1", userId, "p1", "view")
        ));

        when(actionRepository.findByProductId("p1")).thenReturn(Arrays.asList(
            new UserAction("a1", userId, "p1", "view")
        ));

        // When
        List<ProductDoc> result = enhancedRecommendationService.getCollaborativeRecommendations(userId, 10);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getContentBasedRecommendations_WithNoPreferences_ShouldReturnEmpty() {
        // Given
        String userId = "user1";
        when(hashOperations.entries(anyString())).thenReturn(java.util.Map.of());

        // When
        List<ProductDoc> result = enhancedRecommendationService.getContentBasedRecommendations(userId, 5);

        // Then
        assertTrue(result.isEmpty());
        verify(hashOperations).entries(anyString());
    }
}
