package org.dynamcorp.handsaiv2.dto;

import org.dynamcorp.handsaiv2.model.ApiKeyLocationEnum;
import org.dynamcorp.handsaiv2.model.AuthenticationTypeEnum;

import java.util.List;
import java.util.Map;

public record ImportApiProviderRequest(
        String name,
        String code,
        String baseUrl,
        AuthenticationTypeEnum authenticationType,
        ApiKeyLocationEnum apiKeyLocation,
        String apiKeyName,
        String apiKeyValue,
        Boolean isDynamicAuth,
        String dynamicAuthUrl,
        org.dynamcorp.handsaiv2.model.DynamicAuthMethodEnum dynamicAuthMethod,
        org.dynamcorp.handsaiv2.model.DynamicAuthPayloadTypeEnum dynamicAuthPayloadType,
        org.dynamcorp.handsaiv2.model.DynamicAuthPayloadLocationEnum dynamicAuthPayloadLocation,
        String dynamicAuthPayload,
        String dynamicAuthTokenExtractionPath,
        String dynamicAuthInvalidationKeywords,
        Map<String, String> customHeaders,
        List<ImportApiToolRequest> tools) {
}
