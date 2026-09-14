package com.mtcrm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtcrm.common.api.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout",
            "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password", "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification");
    private static final int MAX_TRACKED_WINDOWS = 20_000;
    private record Window(long startedAt, int count) {}
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowSeconds;
    private final ObjectMapper mapper;

    public AuthRateLimitFilter(ObjectMapper mapper,
                               @Value("${app.security.auth-rate-limit:20}") int maxRequests,
                               @Value("${app.security.auth-rate-window-seconds:60}") long windowSeconds) {
        this.mapper = mapper;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LIMITED_PATHS.contains(requestPath(request)) || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String client = request.getRemoteAddr();
        String path = requestPath(request);
        String key = client + ':' + path;
        long now = Instant.now().getEpochSecond();
        if (!windows.containsKey(key) && windows.size() >= MAX_TRACKED_WINDOWS) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= windowSeconds);
            if (windows.size() >= MAX_TRACKED_WINDOWS) {
            writeLimited(response, path, windowSeconds);
                return;
            }
        }
        Window current = windows.compute(key, (ignored, old) ->
                old == null || now - old.startedAt >= windowSeconds ? new Window(now, 1) : new Window(old.startedAt, old.count + 1));
        if (current.count > maxRequests) {
            writeLimited(response, path, Math.max(1, windowSeconds - (now - current.startedAt)));
            return;
        }
        chain.doFilter(request, response);
        if (windows.size() > 10_000) windows.entrySet().removeIf(e -> now - e.getValue().startedAt > windowSeconds);
    }

    private static String requestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return servletPath == null || servletPath.isBlank() ? request.getRequestURI() : servletPath;
    }

    private void writeLimited(HttpServletResponse response, String path, long retryAfter) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType("application/json");
        String traceId = MDC.get("traceId");
        mapper.writeValue(response.getOutputStream(), new ApiError(Instant.now(), 429, "RATE_LIMITED",
                "Too many authentication attempts", path, traceId == null ? "" : traceId, List.of()));
    }
}
