package com.mtcrm.security;

import com.mtcrm.config.AppProperties;
import com.mtcrm.user.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final AppProperties properties;
    private final SecretKey key;

    public JwtService(AppProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.jwt().issuer())
                .subject(user.getId().toString())
                .claim("tenantId", user.getTenantId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("sessionVersion", user.getSessionVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.jwt().accessTtl())))
                .signWith(key)
                .compact();
    }

    public CurrentUser parseAccessToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer(properties.jwt().issuer()).build()
                .parseSignedClaims(token).getPayload();
        return new CurrentUser(UUID.fromString(claims.getSubject()),
                UUID.fromString(claims.get("tenantId", String.class)),
                claims.get("email", String.class),
                com.mtcrm.user.Role.valueOf(claims.get("role", String.class)),
                claims.get("sessionVersion", Long.class));
    }

    public long accessExpiresInSeconds() { return properties.jwt().accessTtl().toSeconds(); }
}
