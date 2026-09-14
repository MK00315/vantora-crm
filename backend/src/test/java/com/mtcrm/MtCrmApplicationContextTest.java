package com.mtcrm;

import org.junit.jupiter.api.Test;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MtCrmApplicationContextTest {
    @Autowired MultipartConfigElement multipart;

    @Test
    void completeApplicationContextStarts() {
        // Startup itself verifies the real security filter chain and application wiring.
    }

    @Test
    void multipartRequestLimitLeavesRoomForTenMegabyteFileOverhead() {
        org.assertj.core.api.Assertions.assertThat(multipart.getMaxFileSize()).isEqualTo(10L * 1024 * 1024);
        org.assertj.core.api.Assertions.assertThat(multipart.getMaxRequestSize()).isEqualTo(11L * 1024 * 1024);
    }
}
