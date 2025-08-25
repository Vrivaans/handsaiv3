package org.dynamcorp.handsaiv2.dto;

import org.dynamcorp.handsaiv2.model.ApiKeyLocationEnum;
import org.dynamcorp.handsaiv2.model.AuthenticationTypeEnum;
import org.dynamcorp.handsaiv2.model.HttpMethodEnum;

import java.util.List;

public record UpdateApiToolRequest(
    String name,
    String description,
    String baseUrl,
    String endpointPath,
    HttpMethodEnum httpMethod,
    boolean enabled,
    AuthenticationTypeEnum authenticationType,
    ApiKeyLocationEnum apiKeyLocation,
    String apiKeyName,
    String apiKeyValue,
    List<ToolParameterRequest> parameters
) {}
