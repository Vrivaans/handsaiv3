package org.dynamcorp.handsaiv2.service;

import org.dynamcorp.handsaiv2.dto.ApiToolResponse;
import org.dynamcorp.handsaiv2.dto.CreateApiToolRequest;
import org.dynamcorp.handsaiv2.dto.UpdateApiToolRequest;
import org.dynamcorp.handsaiv2.model.ApiTool;

import java.util.List;

public interface ApiToolService {

    ApiToolResponse createApiTool(CreateApiToolRequest request);

    List<ApiToolResponse> createApiToolsBatch(List<CreateApiToolRequest> requests);

    ApiToolResponse updateApiTool(Long id, UpdateApiToolRequest request);

    ApiToolResponse getApiTool(Long id);

    List<ApiToolResponse> getAllApiTools();

    void deleteApiTool(Long id);

    ApiToolResponse validateApiToolHealth(Long id);

    ApiTool getApiToolByCode(String code);
}
