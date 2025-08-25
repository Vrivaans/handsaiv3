package org.dynamcorp.handsaiv2.dto;

import org.dynamcorp.handsaiv2.model.ApiKeyLocationEnum;
import org.dynamcorp.handsaiv2.model.AuthenticationTypeEnum;
import org.dynamcorp.handsaiv2.model.HttpMethodEnum;

import java.util.List;

/**
 * DTO for creating a new API tool.
 * The API key value is optional and can be provided at runtime during execution if not set here.
 */
public record CreateApiToolRequest(
    String name,
    String code,
    Boolean enabled,
    String description,
    String baseUrl,
    String endpointPath,
    HttpMethodEnum httpMethod,
    AuthenticationTypeEnum authenticationType,
    ApiKeyLocationEnum apiKeyLocation,
    String apiKeyName,
    /**
     * Optional. The API key or token to be stored for the tool.
     * If null, the key must be provided at execution time.
     */
    String apiKeyValue,
    List<ToolParameterRequest> parameters
) {}
