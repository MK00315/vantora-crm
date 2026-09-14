package com.mtcrm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

@Configuration
public class CacheConfig implements CachingConfigurer {
    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    RedisSerializer<Object> cacheValueSerializer(ObjectMapper objectMapper) {
        return GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(objectMapper.copy())
                .defaultTyping(true)
                .build();
    }

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, RedisSerializer<Object> cacheValueSerializer) {
        var base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(cacheValueSerializer))
                .entryTtl(Duration.ofMinutes(5));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withCacheConfiguration("dashboard", base.entryTtl(Duration.ofSeconds(60)))
                .withCacheConfiguration("customers", base.entryTtl(Duration.ofMinutes(10)))
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            public void handleCacheGetError(RuntimeException e, Cache c, Object k) { warn("read", c, e); }
            public void handleCachePutError(RuntimeException e, Cache c, Object k, Object v) { warn("write", c, e); }
            public void handleCacheEvictError(RuntimeException e, Cache c, Object k) { warn("evict", c, e); }
            public void handleCacheClearError(RuntimeException e, Cache c) { warn("clear", c, e); }
            private void warn(String operation, Cache cache, RuntimeException error) {
                log.warn("Redis cache {} failed for {}. Continuing with PostgreSQL: {}", operation, cache.getName(), error.getMessage());
            }
        };
    }
}
