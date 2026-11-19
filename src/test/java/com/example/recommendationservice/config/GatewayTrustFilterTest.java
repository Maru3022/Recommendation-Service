package com.example.recommendationservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayTrustFilterTest {
    @Test void rejectsForgedIdentityFromDirectRequest() throws Exception {
        var request = new MockHttpServletRequest(); request.addHeader("X-User-Id", "victim");
        var response = new MockHttpServletResponse();
        new GatewayTrustFilter("gateway-secret").doFilter(request, response, new MockFilterChain());
        assertEquals(403, response.getStatus());
    }
}
