package org.dynamcorp.handsaiv2.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record McpTool(
        String name,
        String description,
        Map<String, Object> inputSchema) {
}
