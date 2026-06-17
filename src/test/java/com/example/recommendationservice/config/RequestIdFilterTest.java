package com.example.recommendationservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

public class RequestIdFilterTest {

    @Test
    void filter_SetsRequestIdHeaderWhenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        RequestIdFilter filter = new RequestIdFilter();

        org.springframework.web.server.WebFilterChain chain = ex -> Mono.empty();

        filter.filter(exchange, chain).block();

        assertNotNull(exchange.getResponse().getHeaders().getFirst(RequestIdFilter.HEADER_NAME));
    }
}
