package org.dynamcorp.handsaiv2.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McpError {
    private final int code;
    private final String message;
}
