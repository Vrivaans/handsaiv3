package org.dynamcorp.handsaiv2.dto;

import org.dynamcorp.handsaiv2.model.ApiKeyLocationEnum;
import org.dynamcorp.handsaiv2.model.ApiProvider;
import org.dynamcorp.handsaiv2.model.AuthenticationTypeEnum;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.dynamcorp.handsaiv2.model.DynamicAuthMethodEnum;
import org.dynamcorp.handsaiv2.model.DynamicAuthPayloadLocationEnum;
import org.dynamcorp.handsaiv2.model.DynamicAuthPayloadTypeEnum;
import java.util.Map;
import java.util.HashMap;

public record ApiProviderResponse(
        Long id,
        String name,
        String code,
        String baseUrl,
        AuthenticationTypeEnum authenticationType,
        ApiKeyLocationEnum apiKeyLocation,
        String apiKeyName,
        boolean isExportable,
        Map<String, String> customHeaders,
        boolean isDynamicAuth,
        String dynamicAuthUrl,
        DynamicAuthMethodEnum dynamicAuthMethod,
        String dynamicAuthPayload,
        DynamicAuthPayloadTypeEnum dynamicAuthPayloadType,
        DynamicAuthPayloadLocationEnum dynamicAuthPayloadLocation,
        String dynamicAuthTokenExtractionPath,
        String dynamicAuthInvalidationKeywords) {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ApiProviderResponse from(ApiProvider provider) {
        Map<String, String> headers = new HashMap<>();
        if (provider.getCustomHeadersJson() != null && !provider.getCustomHeadersJson().isEmpty()) {
            try {
                headers = objectMapper.readValue(provider.getCustomHeadersJson(),
                        new TypeReference<Map<String, String>>() {
                        });
                headers.replaceAll((k, v) -> (v != null && !v.isBlank()) ? "generic_text" : v);
            } catch (Exception e) {
                // Return empty map on parse error
            }
        }

        String obscuredDynamicAuthPayload = provider.getDynamicAuthPayload();
        if (obscuredDynamicAuthPayload != null && !obscuredDynamicAuthPayload.isBlank()) {
            try {
                Map<String, String> payloadMap = objectMapper.readValue(obscuredDynamicAuthPayload,
                        new TypeReference<Map<String, String>>() {
                        });
                payloadMap.replaceAll((k, v) -> (v != null && !v.isBlank()) ? "generic_text" : v);
                obscuredDynamicAuthPayload = objectMapper.writeValueAsString(payloadMap);
            } catch (Exception e) {
                // Ignore parse errors here, let the raw string be returned if not valid JSON
            }
        }

        return new ApiProviderResponse(
                provider.getId(),
                provider.getName(),
                provider.getCode(),
                provider.getBaseUrl(),
                provider.getAuthenticationType(),
                provider.getApiKeyLocation(),
                provider.getApiKeyName(),
                provider.isExportable(),
                headers,
                provider.isDynamicAuth(),
                provider.getDynamicAuthUrl(),
                provider.getDynamicAuthMethod(),
                obscuredDynamicAuthPayload,
                provider.getDynamicAuthPayloadType(),
                provider.getDynamicAuthPayloadLocation(),
                provider.getDynamicAuthTokenExtractionPath(),
                provider.getDynamicAuthInvalidationKeywords());
    }
}
