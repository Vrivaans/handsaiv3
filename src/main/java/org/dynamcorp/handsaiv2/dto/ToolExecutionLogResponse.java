package org.dynamcorp.handsaiv2.dto;

import org.dynamcorp.handsaiv2.model.ToolExecutionLog;

import java.time.Instant;

public record ToolExecutionLogResponse(
        Long id,
        String toolName,
        String sessionId,
        String requestPayload,
        String responsePayload,
        Long executionTimeMs,
        boolean success,
        String errorMessage,
        Instant executedAt) {
    public static ToolExecutionLogResponse from(ToolExecutionLog log) {
        return new ToolExecutionLogResponse(
                log.getId(),
                log.getApiTool() != null ? log.getApiTool().getName() : "Unknown Tool",
                log.getSessionId(),
                log.getRequestPayload(),
                log.getResponsePayload(),
                log.getExecutionTimeMs(),
                log.isSuccess(),
                log.getErrorMessage(),
                log.getExecutedAt());
    }
}
