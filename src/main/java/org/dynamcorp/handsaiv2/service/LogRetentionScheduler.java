package org.dynamcorp.handsaiv2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dynamcorp.handsaiv2.repository.ToolExecutionLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogRetentionScheduler {

    private final ToolExecutionLogRepository logRepository;

    @Value("${handsai.analytics.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM
    @Transactional
    public void cleanUpOldLogs() {
        log.info("Starting routine cleanup of execution logs older than {} days", retentionDays);
        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        try {
            int deletedCount = logRepository.deleteByExecutedAtBefore(cutoffDate);
            log.info("Successfully deleted {} old execution logs", deletedCount);
        } catch (Exception e) {
            log.error("Error during execution logs cleanup: {}", e.getMessage(), e);
        }
    }
}
