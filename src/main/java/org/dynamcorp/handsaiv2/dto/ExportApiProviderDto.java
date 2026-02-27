package org.dynamcorp.handsaiv2.dto;

import org.dynamcorp.handsaiv2.model.ApiKeyLocationEnum;
import org.dynamcorp.handsaiv2.model.AuthenticationTypeEnum;
import java.util.List;
import java.util.Map;

public record ExportApiProviderDto(
        String name,
        String code,
        String baseUrl,
        AuthenticationTypeEnum authenticationType,
        ApiKeyLocationEnum apiKeyLocation,
        String apiKeyName,
        String apiKeyValue,
        Map<String, String> customHeaders,
        List<ExportApiToolDto> tools) {
}
