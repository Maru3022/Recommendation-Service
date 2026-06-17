package com.example.recommendationservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FallbackControllerTest {

    @Test
    void fallback_ReturnsServiceUnavailableWithUnknownCorrelationId() {
        FallbackController controller = new FallbackController();

        ResponseEntity<Map<String, String>> response = controller.fallback(null);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Service temporarily unavailable", response.getBody().get("error"));
        assertEquals("unknown", response.getBody().get("correlationId"));
    }
}
