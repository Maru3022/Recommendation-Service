package com.example.recommendationservice;

import com.example.recommendationservice.controller.UserActionController;
import com.example.recommendationservice.model.UserAction;
import com.example.recommendationservice.service.UserActionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@WebFluxTest(UserActionController.class)
public class UserActionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UserActionService userActionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void trackUserAction_WithValidAction_ShouldReturnOk() throws Exception {
        // Given
        UserAction action = new UserAction("action1", "user1", "product1", "view");

        // When & Then
        webTestClient.post()
                .uri("/api/user-actions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(action))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getUserActionHistory_WithValidUserId_ShouldReturnActions() {
        // Given
        String userId = "user1";
        List<UserAction> actions = Arrays.asList(
            new UserAction("action1", userId, "product1", "view"),
            new UserAction("action2", userId, "product2", "like")
        );
        when(userActionService.getUserActionHistory(userId)).thenReturn(actions);

        // When & Then
        webTestClient.get()
                .uri("/api/user-actions/{userId}/history", userId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$[0].userId").isEqualTo(userId)
                .jsonPath("$[0].productId").isEqualTo("product1")
                .jsonPath("$[0].actionType").isEqualTo("view");
                
        verify(userActionService).getUserActionHistory(userId);
    }
}
