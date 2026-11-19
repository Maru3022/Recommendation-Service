package com.example.recommendationservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@Profile("!test")
public class GatewayTrustFilter extends OncePerRequestFilter {
    private final String expectedSecret;
    public GatewayTrustFilter(@Value("${internal.gateway.secret:}") String expectedSecret) { this.expectedSecret = expectedSecret; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean identity = request.getHeader("X-User-Id") != null || request.getHeader("X-User-Role") != null;
        String supplied = request.getHeader("X-Internal-Gateway-Secret");
        boolean trusted = supplied != null && !expectedSecret.isBlank() && MessageDigest.isEqual(expectedSecret.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
        if (identity && !trusted) { response.sendError(403); return; }
        chain.doFilter(request, response);
    }
}
