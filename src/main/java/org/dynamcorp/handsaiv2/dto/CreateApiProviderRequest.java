package org.dynamcorp.handsaiv2.dto;

import org.dynamcorp.handsaiv2.model.ApiKeyLocationEnum;
import org.dynamcorp.handsaiv2.model.AuthenticationTypeEnum;

public record CreateApiProviderRequest(
                String name,
                String code,
                String baseUrl,
                AuthenticationTypeEnum authenticationType,
                ApiKeyLocationEnum apiKeyLocation,
                String apiKeyName,
                String apiKeyValue) {
}
