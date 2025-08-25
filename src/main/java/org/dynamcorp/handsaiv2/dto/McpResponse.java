package org.dynamcorp.handsaiv2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpResponse<T> {
    private final String jsonrpc;
    private final T result;
    private final McpError error;
    private final String id;
}
