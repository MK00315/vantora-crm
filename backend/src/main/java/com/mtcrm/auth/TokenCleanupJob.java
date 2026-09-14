package com.mtcrm.auth;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
public class TokenCleanupJob {
    private final RefreshTokenRepository refreshTokens;
    private final ActionTokenRepository actionTokens;
    private final PendingRegistrationRepository pendingRegistrations;
    private final Duration retention;
    private final Duration pendingRetention;

    public TokenCleanupJob(RefreshTokenRepository refreshTokens, ActionTokenRepository actionTokens,
                           PendingRegistrationRepository pendingRegistrations,
                           @Value("${app.security.token-retention:7d}") Duration retention,
                           @Value("${app.security.pending-registration-retention:24h}") Duration pendingRetention) {
        this.refreshTokens = refreshTokens;
        this.actionTokens = actionTokens;
        this.pendingRegistrations = pendingRegistrations;
        this.retention = retention;
        this.pendingRetention = pendingRetention;
    }

    @Scheduled(cron = "${app.security.token-cleanup-cron:0 30 2 * * *}")
    @SchedulerLock(name = "retired-token-cleanup", lockAtMostFor = "15m", lockAtLeastFor = "5s")
    @Transactional
    public void deleteRetiredTokens() {
        Instant cutoff = Instant.now().minus(retention);
        refreshTokens.deleteRetiredBefore(cutoff);
        actionTokens.deleteRetiredBefore(cutoff);
        pendingRegistrations.deleteCreatedBefore(Instant.now().minus(pendingRetention));
    }
}
