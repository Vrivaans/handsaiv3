package org.dynamcorp.handsaiv2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpResponse<T>(
        String jsonrpc,
        T result,
        McpError error,
        String id) {
}
