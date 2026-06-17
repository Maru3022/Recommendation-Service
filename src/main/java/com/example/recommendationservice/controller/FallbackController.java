package com.example.recommendationservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FallbackController {

    @GetMapping(value = "/fallback", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> fallback(@RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service temporarily unavailable",
                        "correlationId", correlationId != null ? correlationId : "unknown"
                ));
    }
}
