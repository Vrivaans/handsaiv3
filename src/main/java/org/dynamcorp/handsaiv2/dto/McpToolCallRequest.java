package org.dynamcorp.handsaiv2.dto;

public record McpToolCallRequest(
        String jsonrpc,
        String method,
        McpToolCallParams params,
        String id) {
}
