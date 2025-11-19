package org.dynamcorp.handsaiv2.dto;

import java.util.Map;

public record McpToolCallParams(
        String name,
        Map<String, Object> arguments) {
}
