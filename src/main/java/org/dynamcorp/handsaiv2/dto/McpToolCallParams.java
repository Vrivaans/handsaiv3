package org.dynamcorp.handsaiv2.dto;

import lombok.Data;

import java.util.Map;

@Data
public class McpToolCallParams {
    private String name;
    private Map<String, Object> arguments;
}
