package org.dynamcorp.handsaiv2.dto;

import java.util.Map;

public record ToolExecuteRequest(
    String toolName,
    Map<String, Object> parameters,
    String sessionId
) {}
