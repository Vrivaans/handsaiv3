package org.dynamcorp.handsaiv2.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record McpToolCallResponse(
        List<McpContent> content) {
}
