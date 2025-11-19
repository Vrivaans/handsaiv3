package org.dynamcorp.handsaiv2.dto;

import lombok.Builder;

@Builder
public record McpContent(
        String type,
        String text) {
}
