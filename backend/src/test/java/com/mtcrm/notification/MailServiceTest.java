package com.mtcrm.notification;

import com.mtcrm.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MailServiceTest {
    @Test
    void actionTokensUseUrlFragmentsSoProxiesDoNotLogSecrets() {
        JavaMailSender sender = mock(JavaMailSender.class);
        var properties = new AppProperties(null, null, null,
                new AppProperties.Mail(true, "noreply@example.test"), null, null);
        var service = new MailService(sender, properties, "https://crm.example.test", Runnable::run);

        service.welcome("admin@example.test", "Asha", "verify-secret");
        service.passwordReset("admin@example.test", "reset-secret");

        var messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender, times(2)).send(messages.capture());
        assertThat(messages.getAllValues().get(0).getText())
                .contains("/verify-email#token=verify-secret")
                .doesNotContain("?token=");
        assertThat(messages.getAllValues().get(1).getText())
                .contains("/reset-password#token=reset-secret")
                .doesNotContain("?token=");
    }
}
