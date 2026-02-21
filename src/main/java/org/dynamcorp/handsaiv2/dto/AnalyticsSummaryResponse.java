package org.dynamcorp.handsaiv2.dto;

public record AnalyticsSummaryResponse(
        long totalExecutions,
        long successfulExecutions,
        double successRatePercentage,
        double averageLatencyMs) {
}
