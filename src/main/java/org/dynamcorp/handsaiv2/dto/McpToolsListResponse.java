package org.dynamcorp.handsaiv2.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class McpToolsListResponse {
    private final List<McpTool> tools;
}
