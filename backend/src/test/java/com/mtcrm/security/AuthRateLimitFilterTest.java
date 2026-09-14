package com.mtcrm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {
    @AfterEach void clearMdc() { MDC.clear(); }

    @Test
    void rateLimitUsesStandardCorrelatedApiError() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        AuthRateLimitFilter filter = new AuthRateLimitFilter(mapper, 1, 60);
        var first = request();
        filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());
        MDC.put("traceId", "rate-test-123");
        var limited = new MockHttpServletResponse();

        filter.doFilter(request(), limited, new MockFilterChain());

        assertThat(limited.getStatus()).isEqualTo(429);
        var json = mapper.readTree(limited.getContentAsByteArray());
        assertThat(json.path("status").asInt()).isEqualTo(429);
        assertThat(json.path("code").asText()).isEqualTo("RATE_LIMITED");
        assertThat(json.path("path").asText()).isEqualTo("/api/v1/auth/login");
        assertThat(json.path("traceId").asText()).isEqualTo("rate-test-123");
        assertThat(json.path("violations").isArray()).isTrue();
    }

    @Test
    void ignoresUnknownAuthenticationPaths() throws Exception {
        var filter = new AuthRateLimitFilter(new ObjectMapper().findAndRegisterModules(), 1, 60);
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/random-path");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rateLimitsAnonymousLogoutDatabaseLookups() throws Exception {
        var filter = new AuthRateLimitFilter(new ObjectMapper().findAndRegisterModules(), 1, 60);
        var first = new MockHttpServletRequest("POST", "/api/v1/auth/logout");
        first.setRemoteAddr("192.0.2.20");
        filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());
        var second = new MockHttpServletRequest("POST", "/api/v1/auth/logout");
        second.setRemoteAddr("192.0.2.20");
        var response = new MockHttpServletResponse();

        filter.doFilter(second, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void forwardedPrefixCannotBypassAuthenticationRateLimit() throws Exception {
        var filter = new AuthRateLimitFilter(new ObjectMapper().findAndRegisterModules(), 1, 60);
        var first = forwardedRequest("/spoofed/api/v1/auth/login", "/api/v1/auth/login");
        filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());
        var response = new MockHttpServletResponse();

        filter.doFilter(forwardedRequest("/spoofed/api/v1/auth/login", "/api/v1/auth/login"), response,
                new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
    }

    private static MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.0.2.10");
        return request;
    }

    private static MockHttpServletRequest forwardedRequest(String requestUri, String servletPath) {
        var request = new MockHttpServletRequest("POST", requestUri);
        request.setServletPath(servletPath);
        request.setRemoteAddr("192.0.2.30");
        return request;
    }
}
