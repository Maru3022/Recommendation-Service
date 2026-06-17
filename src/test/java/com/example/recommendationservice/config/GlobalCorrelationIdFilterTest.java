package com.example.recommendationservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalCorrelationIdFilterTest {

    @Test
    void filter_SetsCorrelationIdHeaderWhenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GlobalCorrelationIdFilter filter = new GlobalCorrelationIdFilter();

        // simple chain that does nothing
        org.springframework.cloud.gateway.filter.GatewayFilterChain chain = ex -> Mono.empty();

        filter.filter(exchange, chain).block();

        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertNotNull(headers.getFirst(GlobalCorrelationIdFilter.CORRELATION_ID_HEADER));
    }
}
