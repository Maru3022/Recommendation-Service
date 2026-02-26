package com.example.recommendationservice;

import com.example.recommendationservice.controller.GlobalExceptionHandler;
import com.example.recommendationservice.model.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRedisException_ShouldReturn503(){
        RedisConnectionFailureException ex = new RedisConnectionFailureException("Conn failed");


        ResponseEntity<ErrorResponse> response = handler.handleRedisException(ex);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Personalization data source error", response.getBody().getMessage());
    }
}
