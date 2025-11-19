package org.dynamcorp.handsaiv2.dto;

import lombok.Builder;

@Builder
public record McpError(
        int code,
        String message) {
}
