package com.example.recommendationservice;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.model.RecommendationResponse;
import com.example.recommendationservice.repository.ProductSearchRepository;
import com.example.recommendationservice.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecommendationServiceTest {

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private RecommendationService recommendationService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setUp(){
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getRecommendations_WithPersonalization_ShouldReturnCategorizedProducts(){
        String userId = "123";
        String favCategory = "Electronics";
        when(valueOperations.get("user:" + userId + ":fav_category")).thenReturn(favCategory);

        List<ProductDoc> products = List.of(new ProductDoc("1","Phone","Electronics",999.0,"url"));
        Page<ProductDoc> page = new PageImpl<>(products);

        when(productSearchRepository.findByCategory(eq(favCategory), any())).thenReturn(page);

        RecommendationResponse response = recommendationService.getRecommendations(userId,0,10);

        assertNotNull(response);
        assertEquals(1,response.getProducts().size());
        assertEquals("Electronics",response.getProducts().get(0).getCategory());
        verify(productSearchRepository).findByCategory(eq(favCategory), any());
    }

    @Test
    void getRecommendations_WithoutPersonalization_ShouldReturnAllProducts(){
        String userId = "456";
        when(valueOperations.get(anyString())).thenReturn(null);

        Page<ProductDoc> page = new PageImpl<>(List.of());
        when(productSearchRepository.findAll(any(PageRequest.class))).thenReturn(page);

        recommendationService.getRecommendations(userId,0,10);

        verify(productSearchRepository).findAll(any(PageRequest.class));
        verify(productSearchRepository,never()).findByCategory(anyString(),any());
    }
}
