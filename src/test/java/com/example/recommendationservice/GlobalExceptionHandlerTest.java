package com.example.recommendationservice;

import com.example.recommendationservice.controller.GlobalExceptionHandler;
import com.example.recommendationservice.model.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.NoSuchIndexException;
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

    @Test
    void handleIllegalArgumentException_ShouldReturn400(){
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("size must be between 1 and 100")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("size must be between 1 and 100", response.getBody().getMessage());
    }

    @Test
    void handleElasticsearchException_ShouldReturn500(){
        ResponseEntity<ErrorResponse> response = handler.handleElasticsearchException(new NoSuchIndexException("products"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Search service is temporarily unavailable", response.getBody().getMessage());
    }
}
