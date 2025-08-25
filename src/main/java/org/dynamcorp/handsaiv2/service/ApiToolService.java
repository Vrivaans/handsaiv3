package org.dynamcorp.handsaiv2.service;

import org.dynamcorp.handsaiv2.dto.ApiToolResponse;
import org.dynamcorp.handsaiv2.dto.CreateApiToolRequest;
import org.dynamcorp.handsaiv2.dto.UpdateApiToolRequest;
import org.dynamcorp.handsaiv2.model.ApiTool;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ApiToolService {

    CompletableFuture<ApiToolResponse> createApiTool(CreateApiToolRequest request);

    CompletableFuture<ApiToolResponse> updateApiTool(Long id, UpdateApiToolRequest request);

    CompletableFuture<ApiToolResponse> getApiTool(Long id);

    CompletableFuture<List<ApiToolResponse>> getAllApiTools();

    CompletableFuture<Void> deleteApiTool(Long id);

    CompletableFuture<ApiToolResponse> validateApiToolHealth(Long id);

    CompletableFuture<ApiTool> getApiToolByCode(String code);
}
