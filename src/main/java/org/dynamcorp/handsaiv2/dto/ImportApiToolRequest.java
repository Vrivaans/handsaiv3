package org.dynamcorp.handsaiv2.dto;

import org.dynamcorp.handsaiv2.model.HttpMethodEnum;

import java.util.List;

public record ImportApiToolRequest(
        String name,
        String code,
        String description,
        String endpointPath,
        HttpMethodEnum httpMethod,
        List<ImportToolParameterRequest> parameters) {
}
