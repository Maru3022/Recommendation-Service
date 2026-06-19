package com.example.recommendationservice.controller;

import com.example.recommendationservice.model.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.data.elasticsearch.NoSuchIndexException.class)
    public ResponseEntity<ErrorResponse> handleElasticsearchException(Exception ex) {
        log.error("Elasticsearch index not found: {}", ex.getMessage());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Search service is temporarily unavailable");
    }

    @ExceptionHandler(org.springframework.data.redis.RedisConnectionFailureException.class)
    public ResponseEntity<ErrorResponse> handleRedisException(Exception ex) {
        log.warn("Redis connection failed, personalization may be degraded: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Personalization data source error");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse("Validation error");
        log.warn("Constraint violation: {}", message);
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Validation error");
        log.warn("Method argument not valid: {}", message);
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: ", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred");
    }

    // -------------------------------------------------------------------------

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return new ResponseEntity<>(
                new ErrorResponse(status.value(), message, System.currentTimeMillis()),
                status);
    }
}
