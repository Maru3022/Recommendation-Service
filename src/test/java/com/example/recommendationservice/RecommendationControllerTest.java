package com.example.recommendationservice;

import com.example.recommendationservice.controller.RecommendationController;
import com.example.recommendationservice.model.RecommendationResponse;
import com.example.recommendationservice.service.RecommendationService;
import com.example.recommendationservice.service.EnhancedRecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.mockito.Mockito.when;


@WebFluxTest(RecommendationController.class)
public class RecommendationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private EnhancedRecommendationService enhancedRecommendationService;

    @Test
    void getRecommendations_ShouldReturnOk() throws Exception{
        RecommendationResponse mockResponse = new RecommendationResponse(List.of(),0,0,false);

        when(recommendationService.getRecommendations("user1",0,10))
                .thenReturn(mockResponse);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/recommendations/user1")
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.currentPage").isEqualTo(0)
                .jsonPath("$.products").isArray();
    }

    @Test
    void getRecommendations_WithInvalidPage_ShouldReturnBadRequest() {
        // Validation annotation @Min(0) will catch this before service is called
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/recommendations/user1")
                        .queryParam("page", "-1")
                        .queryParam("size", "10")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getPopular_ShouldReturnOk() {
        when(recommendationService.getPopularProducts(5)).thenReturn(List.of());

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/recommendations/popular")
                        .queryParam("limit", "5")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray();
    }
}
