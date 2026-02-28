package org.dynamcorp.handsaiv2.dto;

import org.dynamcorp.handsaiv2.model.ApiKeyLocationEnum;
import org.dynamcorp.handsaiv2.model.AuthenticationTypeEnum;
import org.dynamcorp.handsaiv2.model.DynamicAuthMethodEnum;
import org.dynamcorp.handsaiv2.model.DynamicAuthPayloadLocationEnum;
import org.dynamcorp.handsaiv2.model.DynamicAuthPayloadTypeEnum;
import java.util.Map;

public record CreateApiProviderRequest(
        String name,
        String code,
        String baseUrl,
        AuthenticationTypeEnum authenticationType,
        ApiKeyLocationEnum apiKeyLocation,
        String apiKeyName,
        String apiKeyValue,
        Boolean isExportable,
        Map<String, String> customHeaders,
        Boolean isDynamicAuth,
        String dynamicAuthUrl,
        DynamicAuthMethodEnum dynamicAuthMethod,
        String dynamicAuthPayload,
        DynamicAuthPayloadTypeEnum dynamicAuthPayloadType,
        DynamicAuthPayloadLocationEnum dynamicAuthPayloadLocation,
        String dynamicAuthTokenExtractionPath,
        String dynamicAuthInvalidationKeywords) {
}
