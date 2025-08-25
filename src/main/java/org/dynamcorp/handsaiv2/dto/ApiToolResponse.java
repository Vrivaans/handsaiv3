package org.dynamcorp.handsaiv2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.dynamcorp.handsaiv2.model.ApiKeyLocationEnum;
import org.dynamcorp.handsaiv2.model.ApiTool;
import org.dynamcorp.handsaiv2.model.AuthenticationTypeEnum;
import org.dynamcorp.handsaiv2.model.HttpMethodEnum;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents the data of an API tool that is safe to expose to clients.
 * It purposefully excludes sensitive information like the stored API key value.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiToolResponse(
        Long id,
        String code,
        String name,
        String description,
        String baseUrl,
        String endpointPath,
        HttpMethodEnum httpMethod,
        boolean enabled,
        boolean healthy,
        Instant lastHealthCheck,
        AuthenticationTypeEnum authenticationType,
        ApiKeyLocationEnum apiKeyLocation,
        String apiKeyName,
        List<ToolParameterResponse> parameters
) {
    public static ApiToolResponse from(ApiTool apiTool) {
        return new ApiToolResponse(
                apiTool.getId(),
                apiTool.getCode(),
                apiTool.getName(),
                apiTool.getDescription(),
                apiTool.getBaseUrl(),
                apiTool.getEndpointPath(),
                apiTool.getHttpMethod(),
                apiTool.isEnabled(),
                apiTool.isHealthy(),
                apiTool.getLastHealthCheck(),
                apiTool.getAuthenticationType(),
                apiTool.getApiKeyLocation(),
                apiTool.getApiKeyName(),
                apiTool.getParameters().stream()
                        .map(ToolParameterResponse::from)
                        .collect(Collectors.toList())
        );
    }
}
