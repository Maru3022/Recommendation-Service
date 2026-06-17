package com.example.recommendationservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalLoggingFilterTest {

    @Test
    void filter_DoesNotThrowAndPropagatesResponseStatus() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GlobalLoggingFilter filter = new GlobalLoggingFilter();

        org.springframework.cloud.gateway.filter.GatewayFilterChain chain = ex -> {
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
    }
}
