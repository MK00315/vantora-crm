package com.mtcrm.notification;

import com.mtcrm.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private final JavaMailSender sender;
    private final AppProperties properties;
    private final String frontendUrl;
    private final Executor mailExecutor;

    public MailService(JavaMailSender sender, AppProperties properties,
                       @Value("${app.frontend-base-url:http://localhost:5173}") String frontendUrl,
                       @Qualifier("mailTaskExecutor") Executor mailExecutor) {
        this.sender = sender;
        this.properties = properties;
        this.frontendUrl = frontendUrl;
        this.mailExecutor = mailExecutor;
    }

    public void welcome(String to, String firstName, String verificationToken) {
        afterCommit(() -> send(to, "Welcome to Vantora CRM",
                "Hi " + firstName + ",\n\nWelcome to Vantora CRM. Verify your email: "
                        + frontendUrl + "/verify-email#token=" + verificationToken));
    }

    public void passwordReset(String to, String token) {
        afterCommit(() -> send(to, "Reset your Vantora CRM password", "Use this secure link to reset your password: "
                + frontendUrl + "/reset-password#token=" + token + "\n\nIf you did not request this, ignore this email."));
    }

    public boolean taskOverdue(String to, String title) {
        return send(to, "Task overdue: " + title, "Your task \"" + title + "\" is overdue. Sign in to Vantora CRM to update it.");
    }

    private void afterCommit(Runnable delivery) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status == STATUS_COMMITTED) dispatch(delivery);
                }
            });
        } else {
            dispatch(delivery);
        }
    }

    private void dispatch(Runnable delivery) {
        try {
            mailExecutor.execute(delivery);
        } catch (RejectedExecutionException ex) {
            log.error("Mail queue is full; delivery was rejected. The user can request a fresh link.");
        }
    }

    private boolean send(String to, String subject, String body) {
        if (!properties.mail().enabled()) {
            log.info("Email disabled; skipped '{}' to {}", subject, to);
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.mail().from());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            return true;
        } catch (MailException ex) {
            log.warn("Email delivery failed for {}: {}", to, ex.getMessage());
            return false;
        }
    }
}
