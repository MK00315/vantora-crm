package com.mtcrm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtcrm.common.api.ApiError;
import com.mtcrm.tenant.TenantContext;
import com.mtcrm.tenant.TenantRepository;
import com.mtcrm.user.UserRepository;
import com.mtcrm.user.UserStatus;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository users;
    private final TenantRepository tenants;
    private final ObjectMapper mapper;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users, TenantRepository tenants,
                                   ObjectMapper mapper) {
        this.jwtService = jwtService;
        this.users = users;
        this.tenants = tenants;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // Streaming responses trigger a second ASYNC dispatch after the request thread has cleared its context.
        // Rebuild authentication from the bearer token so the final dispatch cannot be denied mid-response.
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        try {
            if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
                CurrentUser tokenPrincipal = jwtService.parseAccessToken(header.substring(7));
                var user = users.findByIdAndTenantId(tokenPrincipal.id(), tokenPrincipal.tenantId()).orElse(null);
                if (user == null || user.getStatus() != UserStatus.ACTIVE || user.getEmailVerifiedAt() == null)
                    throw new JwtException("User is not active");
                if (tenants.findById(user.getTenantId()).filter(com.mtcrm.tenant.Tenant::isActive).isEmpty())
                    throw new JwtException("Tenant is not active");
                if (tokenPrincipal.role() != user.getRole()
                        || tokenPrincipal.sessionVersion() != user.getSessionVersion())
                    throw new JwtException("User permissions changed; authenticate again");
                CurrentUser principal = new CurrentUser(user.getId(), user.getTenantId(), user.getEmail(),
                        user.getRole(), user.getSessionVersion());
                TenantContext.set(user.getTenantId());
                var auth = new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ignored) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            String traceId = MDC.get("traceId");
            mapper.writeValue(response.getOutputStream(), new ApiError(Instant.now(), 401, "INVALID_TOKEN",
                    "Access token is invalid or expired", request.getRequestURI(), traceId == null ? "" : traceId, List.of()));
        } finally {
            TenantContext.clear();
        }
    }
}
