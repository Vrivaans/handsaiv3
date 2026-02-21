package org.dynamcorp.handsaiv2.service;

import lombok.RequiredArgsConstructor;
import org.dynamcorp.handsaiv2.dto.AnalyticsSummaryResponse;
import org.dynamcorp.handsaiv2.dto.ToolExecutionLogResponse;
import org.dynamcorp.handsaiv2.model.ToolExecutionLog;
import org.dynamcorp.handsaiv2.repository.ToolExecutionLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ToolExecutionLogRepository logRepository;

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummaryMetrics(int daysToLookBack) {
        Instant fromDate = Instant.now().minus(daysToLookBack, ChronoUnit.DAYS);

        long totalExecutions = logRepository.countByExecutedAtAfter(fromDate);
        if (totalExecutions == 0) {
            return new AnalyticsSummaryResponse(0, 0, 0.0, 0.0);
        }

        long successfulExecutions = logRepository.countBySuccessAndExecutedAtAfter(true, fromDate);
        Double avgTime = logRepository.getAverageExecutionTimeAfter(fromDate);
        double averageLatency = avgTime != null ? avgTime : 0.0;

        double successRate = (double) successfulExecutions / totalExecutions * 100.0;

        return new AnalyticsSummaryResponse(
                totalExecutions,
                successfulExecutions,
                successRate,
                averageLatency);
    }

    @Transactional(readOnly = true)
    public Page<ToolExecutionLogResponse> getExecutionLogs(Pageable pageable) {
        Page<ToolExecutionLog> logs = logRepository.findAllByOrderByExecutedAtDesc(pageable);
        return logs.map(ToolExecutionLogResponse::from);
    }
}
