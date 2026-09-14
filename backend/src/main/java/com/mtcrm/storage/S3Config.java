package com.mtcrm.storage;

import com.mtcrm.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3Config {
    @Bean
    S3Client s3Client(AppProperties properties) {
        var builder = S3Client.builder().region(Region.of(properties.storage().s3().region()));
        String endpoint = properties.storage().s3().endpoint();
        if (endpoint != null && !endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    S3Presigner s3Presigner(AppProperties properties) {
        var builder = S3Presigner.builder().region(Region.of(properties.storage().s3().region()));
        String endpoint = properties.storage().s3().endpoint();
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
            builder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }
}
