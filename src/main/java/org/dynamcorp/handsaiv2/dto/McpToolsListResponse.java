package org.dynamcorp.handsaiv2.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record McpToolsListResponse(
        List<McpTool> tools) {
}
