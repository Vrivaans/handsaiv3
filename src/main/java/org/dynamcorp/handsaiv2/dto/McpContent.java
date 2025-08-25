package org.dynamcorp.handsaiv2.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McpContent {
    private final String type;
    private final String text;
}
