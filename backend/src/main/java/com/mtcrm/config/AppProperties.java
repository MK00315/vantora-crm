package com.mtcrm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.List;
import java.util.Locale;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cookie cookie,
        Cors cors,
        Mail mail,
        Storage storage,
        Demo demo
) {
    public record Jwt(String secret, Duration accessTtl, Duration refreshTtl, Duration actionTtl, String issuer) {
        public Jwt {
            String normalized = secret == null ? "" : secret.toLowerCase(Locale.ROOT);
            if (secret == null || secret.isBlank())
                throw new IllegalArgumentException("JWT_SECRET is required");
            if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
                throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
            if (normalized.contains("change-me") || normalized.contains("must-be-replaced"))
                throw new IllegalArgumentException("JWT_SECRET still contains a known placeholder");
        }
    }
    public record Cookie(boolean secure, String sameSite, String domain) {}
    public record Cors(List<String> allowedOrigins) {}
    public record Mail(boolean enabled, String from) {}
    public record Storage(String provider, String localPath, String publicBaseUrl, S3 s3) {
        public Storage {
            if (provider == null || provider.isBlank())
                throw new IllegalArgumentException("STORAGE_PROVIDER is required");
            provider = provider.toLowerCase(Locale.ROOT);
            if (!provider.equals("local") && !provider.equals("s3"))
                throw new IllegalArgumentException("STORAGE_PROVIDER must be 'local' or 's3'");
            if (provider.equals("s3")) {
                if (s3 == null || s3.bucket() == null || s3.bucket().isBlank())
                    throw new IllegalArgumentException("AWS_S3_BUCKET is required when STORAGE_PROVIDER=s3");
                if (s3.region() == null || !s3.region().matches("[a-z0-9][a-z0-9-]{1,62}"))
                    throw new IllegalArgumentException("AWS_REGION is invalid");
                validateEndpoint(s3.endpoint());
            }
        }

        private static void validateEndpoint(String endpoint) {
            if (endpoint == null || endpoint.isBlank()) return;
            try {
                URI uri = URI.create(endpoint);
                String scheme = uri.getScheme();
                if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                        || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
                    throw new IllegalArgumentException("AWS_S3_ENDPOINT must be an absolute HTTP(S) URL without credentials, query, or fragment");
            } catch (IllegalArgumentException ex) {
                if (ex.getMessage() != null && ex.getMessage().startsWith("AWS_S3_ENDPOINT")) throw ex;
                throw new IllegalArgumentException("AWS_S3_ENDPOINT is invalid", ex);
            }
        }

        public record S3(String bucket, String region, String endpoint) {}
    }
    public record Demo(boolean enabled, String password) {}
}
