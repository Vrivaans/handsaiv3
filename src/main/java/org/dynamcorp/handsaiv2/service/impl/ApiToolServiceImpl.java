package org.dynamcorp.handsaiv2.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dynamcorp.handsaiv2.dto.ApiToolResponse;
import org.dynamcorp.handsaiv2.dto.CreateApiToolRequest;
import org.dynamcorp.handsaiv2.dto.UpdateApiToolRequest;
import org.dynamcorp.handsaiv2.exception.ResourceNotFoundException;
import org.dynamcorp.handsaiv2.model.ApiTool;
import org.dynamcorp.handsaiv2.model.ToolParameter;
import org.dynamcorp.handsaiv2.repository.ApiToolRepository;
import org.dynamcorp.handsaiv2.service.ApiToolService;
import org.dynamcorp.handsaiv2.service.EncryptionService;
import org.dynamcorp.handsaiv2.service.ToolCacheManager;
import org.dynamcorp.handsaiv2.service.ToolValidationService;
import org.dynamcorp.handsaiv2.util.SecurityValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiToolServiceImpl implements ApiToolService {

    private final ApiToolRepository apiToolRepository;
    private final ToolValidationService toolValidationService;
    private final ToolCacheManager toolCacheManager;
    private final EncryptionService encryptionService;
    private final SecurityValidator securityValidator;

    @Override
    @Transactional
    public ApiToolResponse createApiTool(CreateApiToolRequest request) {
        log.info("Creating new API tool: {}", request.name());

        // Validate URL for SSRF
        securityValidator.validateUrl(request.baseUrl());

        if (apiToolRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Tool with code " + request.code() + " already exists");
        }

        ApiTool apiTool = ApiTool.builder()
                .name(request.name())
                .code(request.code())
                .description(request.description())
                .baseUrl(request.baseUrl())
                .endpointPath(request.endpointPath())
                .httpMethod(request.httpMethod())
                .authenticationType(request.authenticationType())
                .apiKeyLocation(request.apiKeyLocation())
                .apiKeyName(request.apiKeyName())
                .apiKeyValue(encryptionService.encrypt(request.apiKeyValue()))
                .enabled(request.enabled() != null ? request.enabled() : true)
                .healthy(false) // Initially false until validated
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        if (request.parameters() != null) {
            List<ToolParameter> parameters = request.parameters().stream()
                    .map(p -> ToolParameter.builder()
                            .apiTool(apiTool)
                            .name(p.name())
                            .code(UUID.randomUUID().toString())
                            .type(p.type())
                            .description(p.description())
                            .required(p.required())
                            .defaultValue(p.defaultValue())
                            .enabled(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build())
                    .collect(Collectors.toList());
            apiTool.setParameters(parameters);
        }

        ApiTool savedTool = apiToolRepository.save(apiTool);

        // Trigger health check synchronously (on virtual thread)
        validateHealth(savedTool);

        // Refresh cache
        toolCacheManager.refreshCache();

        return ApiToolResponse.from(savedTool);
    }

    @Override
    @Transactional
    public ApiToolResponse updateApiTool(Long id, UpdateApiToolRequest request) {
        log.info("Updating API tool with id: {}", id);

        ApiTool apiTool = apiToolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTool not found with id: " + id));

        // Validate URL for SSRF if changed
        if (request.baseUrl() != null) {
            securityValidator.validateUrl(request.baseUrl());
            apiTool.setBaseUrl(request.baseUrl());
        }

        if (request.name() != null)
            apiTool.setName(request.name());
        if (request.description() != null)
            apiTool.setDescription(request.description());
        if (request.endpointPath() != null)
            apiTool.setEndpointPath(request.endpointPath());
        if (request.httpMethod() != null)
            apiTool.setHttpMethod(request.httpMethod());
        if (request.authenticationType() != null)
            apiTool.setAuthenticationType(request.authenticationType());
        if (request.apiKeyLocation() != null)
            apiTool.setApiKeyLocation(request.apiKeyLocation());
        if (request.apiKeyName() != null)
            apiTool.setApiKeyName(request.apiKeyName());
        if (request.apiKeyValue() != null) {
            apiTool.setApiKeyValue(encryptionService.encrypt(request.apiKeyValue()));
        }

        apiTool.setEnabled(request.enabled());
        apiTool.setUpdatedAt(Instant.now());

        // Limpiar parámetros existentes y agregar los nuevos
        if (request.parameters() != null) {
            apiTool.getParameters().clear();
            List<ToolParameter> updatedParameters = request.parameters().stream()
                    .map(paramRequest -> ToolParameter.builder()
                            .name(paramRequest.name())
                            .type(paramRequest.type())
                            .description(paramRequest.description())
                            .required(paramRequest.required())
                            .defaultValue(paramRequest.defaultValue())
                            .apiTool(apiTool)
                            .code(UUID.randomUUID().toString())
                            .enabled(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build())
                    .collect(Collectors.toList());
            apiTool.getParameters().addAll(updatedParameters);
        }

        ApiTool savedTool = apiToolRepository.save(apiTool);

        // Trigger health check
        validateHealth(savedTool);

        // Refresh cache
        toolCacheManager.refreshCache();

        return ApiToolResponse.from(savedTool);
    }

    @Override
    public ApiToolResponse getApiTool(Long id) {
        return apiToolRepository.findById(id)
                .map(ApiToolResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTool not found with id: " + id));
    }

    @Override
    public List<ApiToolResponse> getAllApiTools() {
        return apiToolRepository.findAll().stream()
                .map(ApiToolResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteApiTool(Long id) {
        log.info("Deleting API tool with id: {}", id);
        if (!apiToolRepository.existsById(id)) {
            throw new ResourceNotFoundException("ApiTool not found with id: " + id);
        }
        apiToolRepository.deleteById(id);
        toolCacheManager.refreshCache();
    }

    @Override
    @Transactional
    public ApiToolResponse validateApiToolHealth(Long id) {
        ApiTool apiTool = apiToolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTool not found with id: " + id));
        validateHealth(apiTool);
        return ApiToolResponse.from(apiTool);
    }

    @Override
    public ApiTool getApiToolByCode(String code) {
        return apiToolRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTool not found with code: " + code));
    }

    private void validateHealth(ApiTool apiTool) {
        boolean isHealthy = toolValidationService.validateApiToolHealth(apiTool);
        apiTool.setHealthy(isHealthy);
        apiTool.setLastHealthCheck(Instant.now());
        apiToolRepository.save(apiTool);
    }
}
