package org.dynamcorp.handsaiv2.dto;

import org.dynamcorp.handsaiv2.model.ApiKeyLocationEnum;
import org.dynamcorp.handsaiv2.model.AuthenticationTypeEnum;
import org.dynamcorp.handsaiv2.model.HttpMethodEnum;

import java.util.List;

/**
 * DTO for creating a new API tool.
 * The API key value is optional and can be provided at runtime during execution
 * if not set here.
 */
public record CreateApiToolRequest(
        String name,
        String code,
        Boolean enabled,
        String description,
        Long providerId,
        String endpointPath,
        HttpMethodEnum httpMethod,
        List<ToolParameterRequest> parameters) {
}
