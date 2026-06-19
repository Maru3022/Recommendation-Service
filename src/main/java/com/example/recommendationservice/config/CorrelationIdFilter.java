package com.example.recommendationservice.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that propagates X-Correlation-Id and X-Request-Id headers,
 * injecting them into MDC for structured logging.
 */
@Component
@Order(1)
public class CorrelationIdFilter implements Filter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER  = "X-Request-Id";
    public static final String MDC_CORRELATION    = "correlationId";
    public static final String MDC_REQUEST_ID     = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req = (HttpServletRequest)  request;
        HttpServletResponse res = (HttpServletResponse) response;

        String correlationId = req.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        String requestId = req.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        res.setHeader(CORRELATION_HEADER, correlationId);
        res.setHeader(REQUEST_ID_HEADER, requestId);

        MDC.put(MDC_CORRELATION, correlationId);
        MDC.put(MDC_REQUEST_ID,  requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION);
            MDC.remove(MDC_REQUEST_ID);
        }
    }
}
