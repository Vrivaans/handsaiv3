package org.dynamcorp.handsaiv2.dto;

import lombok.Data;

@Data
public class McpToolCallRequest {
    private String jsonrpc;
    private String method;
    private McpToolCallParams params;
    private String id;
}
