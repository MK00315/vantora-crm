package com.mtcrm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {
    @Test
    void redisJsonSerializerSupportsJavaTimeAndConcreteTypeRoundTrip() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var serializer = new CacheConfig().cacheValueSerializer(mapper);
        CachedValue expected = new CachedValue("tenant-safe", Instant.parse("2026-08-24T00:00:00Z"));

        Object restored = serializer.deserialize(serializer.serialize(expected));

        assertThat(restored).isEqualTo(expected);
    }

    record CachedValue(String label, Instant createdAt) {}
}
