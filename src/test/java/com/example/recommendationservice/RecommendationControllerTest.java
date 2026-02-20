package com.example.recommendationservice;

import com.example.recommendationservice.controller.RecommendationController;
import com.example.recommendationservice.model.RecommendationResponse;
import com.example.recommendationservice.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(RecommendationController.class)
public class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @Test
    void getRecommendations_ShouldReturnOk() throws Exception{
        RecommendationResponse mockResponse = new RecommendationResponse(List.of(),0,0,false);

        when(recommendationService.getRecommendations("user1",0,10))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/recommendations/user1")
                .param("page","0")
                .param("size","10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.products").isArray());
    }
}
