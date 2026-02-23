package org.dynamcorp.handsaiv2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dynamcorp.handsaiv2.model.ToolExecutionLog;
import org.dynamcorp.handsaiv2.repository.ToolExecutionLogRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogBatchProcessor {

    private final ToolExecutionLogRepository logRepository;
    private final Queue<ToolExecutionLog> logQueue = new ConcurrentLinkedQueue<>();

    public void enqueueLog(ToolExecutionLog logEntry) {
        logQueue.offer(logEntry);
    }

    @Scheduled(fixedDelay = 2000)
    public void processBatch() {
        if (logQueue.isEmpty()) {
            return;
        }

        List<ToolExecutionLog> batch = new ArrayList<>();
        ToolExecutionLog logEntry;
        while ((logEntry = logQueue.poll()) != null) {
            batch.add(logEntry);
        }

        if (!batch.isEmpty()) {
            try {
                logRepository.saveAll(batch);
                log.debug("Successfully saved batch of {} execution logs", batch.size());
            } catch (Exception e) {
                log.error("Failed to save batch of execution logs: {}", e.getMessage(), e);
            }
        }
    }
}
