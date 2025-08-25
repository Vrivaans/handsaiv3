package org.dynamcorp.handsaiv2.dto;

public record ToolExecuteResponse(
    boolean success,
    Object result,
    Long executionTimeMs,
    String toolType,
    String errorMessage
) {}
