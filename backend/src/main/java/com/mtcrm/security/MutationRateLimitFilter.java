package com.mtcrm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtcrm.common.api.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MutationRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_LOCAL_WINDOWS = 20_000;
    private static final DefaultRedisScript<List> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local userCount = redis.call('INCR', KEYS[1])
            if userCount == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            local tenantCount = redis.call('INCR', KEYS[2])
            if tenantCount == 1 then redis.call('EXPIRE', KEYS[2], ARGV[1]) end
            return {userCount, tenantCount}
            """, List.class);
    private record Window(long startedAt, int count) {}

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final int perUser;
    private final int perTenant;
    private final long windowSeconds;
    private final ConcurrentHashMap<String, Window> fallback = new ConcurrentHashMap<>();

    public MutationRateLimitFilter(StringRedisTemplate redis, ObjectMapper mapper,
                                   @Value("${app.security.mutation-rate-limit-per-user:300}") int perUser,
                                   @Value("${app.security.mutation-rate-limit-per-tenant:2000}") int perTenant,
                                   @Value("${app.security.mutation-rate-window-seconds:60}") long windowSeconds) {
        this.redis = redis; this.mapper = mapper; this.perUser = perUser;
        this.perTenant = perTenant; this.windowSeconds = windowSeconds;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = requestPath(request);
        return !("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method))
                || !path.startsWith("/api/v1/")
                || path.startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser user)) {
            chain.doFilter(request, response);
            return;
        }
        long bucket = Instant.now().getEpochSecond() / windowSeconds;
        String userKey = "mtcrm:rate:mutation:user:" + user.id() + ':' + bucket;
        String tenantKey = "mtcrm:rate:mutation:tenant:" + user.tenantId() + ':' + bucket;
        Counts counts = increment(userKey, tenantKey);
        if (counts.user() > perUser || counts.tenant() > perTenant) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            response.setContentType("application/json");
            String traceId = MDC.get("traceId");
            String path = requestPath(request);
            mapper.writeValue(response.getOutputStream(), new ApiError(Instant.now(), 429, "RATE_LIMITED",
                    "Workspace mutation rate limit reached", path,
                    traceId == null ? "" : traceId, List.of()));
            return;
        }
        chain.doFilter(request, response);
    }

    private static String requestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return servletPath == null || servletPath.isBlank() ? request.getRequestURI() : servletPath;
    }

    private Counts increment(String userKey, String tenantKey) {
        try {
            List<?> result = redis.execute(INCREMENT_SCRIPT, List.of(userKey, tenantKey), String.valueOf(windowSeconds + 1));
            if (result != null && result.size() == 2)
                return new Counts(((Number) result.get(0)).intValue(), ((Number) result.get(1)).intValue());
        } catch (RuntimeException ignored) {
            // A bounded in-process fallback keeps protection active during a Redis outage.
        }
        return new Counts(localIncrement(userKey), localIncrement(tenantKey));
    }

    private int localIncrement(String key) {
        long now = Instant.now().getEpochSecond();
        if (!fallback.containsKey(key) && fallback.size() >= MAX_LOCAL_WINDOWS) {
            fallback.entrySet().removeIf(entry -> now - entry.getValue().startedAt() >= windowSeconds);
            if (fallback.size() >= MAX_LOCAL_WINDOWS) return Integer.MAX_VALUE;
        }
        Window window = fallback.compute(key, (ignored, old) -> old == null || now - old.startedAt() >= windowSeconds
                ? new Window(now, 1) : new Window(old.startedAt(), old.count() + 1));
        return window.count();
    }

    private record Counts(int user, int tenant) {}
}
