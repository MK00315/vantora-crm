package com.mtcrm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtcrm.user.Role;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class MutationRateLimitFilterTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void boundedFallbackLimitsAuthenticatedMutationsWhenRedisIsUnavailable() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        doThrow(new IllegalStateException("offline")).when(redis).execute(any(), any(), any(String.class));
        var filter = new MutationRateLimitFilter(redis, new ObjectMapper().findAndRegisterModules(), 1, 10, 60);
        var principal = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "a@example.test", Role.COMPANY_ADMIN, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
        var request = new MockHttpServletRequest("POST", "/api/v1/leads");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
        var limited = new MockHttpServletResponse();
        filter.doFilter(request, limited, mock(FilterChain.class));

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getContentAsString()).contains("RATE_LIMITED");
    }

    @Test
    void forwardedPrefixCannotBypassMutationRateLimit() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        doThrow(new IllegalStateException("offline")).when(redis).execute(any(), any(), any(String.class));
        var filter = new MutationRateLimitFilter(redis, new ObjectMapper().findAndRegisterModules(), 1, 10, 60);
        var principal = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "a@example.test", Role.COMPANY_ADMIN, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
        var first = forwardedMutation();
        filter.doFilter(first, new MockHttpServletResponse(), mock(FilterChain.class));
        var response = new MockHttpServletResponse();

        filter.doFilter(forwardedMutation(), response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(429);
    }

    private static MockHttpServletRequest forwardedMutation() {
        var request = new MockHttpServletRequest("POST", "/spoofed/api/v1/leads");
        request.setServletPath("/api/v1/leads");
        return request;
    }
}
