package com.mtcrm.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppPropertiesTest {
    @Test
    void s3StorageRequiresABucketAtStartup() {
        assertThatThrownBy(() -> new AppProperties.Storage("s3", "unused", "https://crm.example.test",
                new AppProperties.Storage.S3("", "ap-south-1", "")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AWS_S3_BUCKET");
    }

    @Test
    void s3StorageRejectsUnsafeEndpointAtStartup() {
        assertThatThrownBy(() -> new AppProperties.Storage("s3", "unused", "https://crm.example.test",
                new AppProperties.Storage.S3("private-bucket", "ap-south-1", "file:///tmp/bucket")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AWS_S3_ENDPOINT");
    }
}
