package org.dynamcorp.handsaiv2.dto;

import org.dynamcorp.handsaiv2.model.ApiKeyLocationEnum;
import org.dynamcorp.handsaiv2.model.ApiProvider;
import org.dynamcorp.handsaiv2.model.AuthenticationTypeEnum;

public record ApiProviderResponse(
        Long id,
        String name,
        String baseUrl,
        AuthenticationTypeEnum authenticationType,
        ApiKeyLocationEnum apiKeyLocation,
        String apiKeyName) {

    public static ApiProviderResponse from(ApiProvider provider) {
        return new ApiProviderResponse(
                provider.getId(),
                provider.getName(),
                provider.getBaseUrl(),
                provider.getAuthenticationType(),
                provider.getApiKeyLocation(),
                provider.getApiKeyName());
    }
}
