package org.dynamcorp.handsaiv2.dto;

import org.dynamcorp.handsaiv2.model.ApiKeyLocationEnum;
import org.dynamcorp.handsaiv2.model.AuthenticationTypeEnum;
import org.dynamcorp.handsaiv2.model.HttpMethodEnum;

import java.util.List;

public record UpdateApiToolRequest(
        String name,
        String code,
        String description,
        Long providerId,
        String endpointPath,
        HttpMethodEnum httpMethod,
        boolean enabled,
        List<ToolParameterRequest> parameters,
        Boolean isExportable) {
}
