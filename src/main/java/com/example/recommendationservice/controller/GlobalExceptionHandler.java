package com.example.recommendationservice.controller;

import com.example.recommendationservice.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.data.elasticsearch.NoSuchIndexException.class)
    public ResponseEntity<ErrorResponse> handleElasticsearchException(Exception ex){
        log.error("CRITICAL: Elasticsearch index not found!. Details: {}",ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Search service is temporality unavailable",
                System.currentTimeMillis()
        );

        return new ResponseEntity<>(error,HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
