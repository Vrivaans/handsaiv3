package org.dynamcorp.handsaiv2.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class McpTool {
    private final String name;
    private final String description;
    private final Map<String, Object> inputSchema;
}
