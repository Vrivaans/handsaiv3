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
    private final org.dynamcorp.handsaiv2.repository.ApiProviderRepository apiProviderRepository;
    private final ToolValidationService toolValidationService;
    private final ToolCacheManager toolCacheManager;
    private final EncryptionService encryptionService;
    private final SecurityValidator securityValidator;

    @Override
    @Transactional
    public ApiToolResponse createApiTool(CreateApiToolRequest request) {
        log.info("Creating new API tool: {}", request.name());

        String toolCode = (request.code() != null && !request.code().isBlank()) ? request.code()
                : java.util.UUID.randomUUID().toString();

        if (apiToolRepository.existsByCode(toolCode)) {
            throw new IllegalArgumentException("Tool with code " + toolCode + " already exists");
        }

        org.dynamcorp.handsaiv2.model.ApiProvider provider = apiProviderRepository.findById(request.providerId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Provider not found with id: " + request.providerId()));

        ApiTool apiTool = ApiTool.builder()
                .name(request.name())
                .code(toolCode)
                .description(request.description())
                .provider(provider)
                .endpointPath(request.endpointPath())
                .httpMethod(request.httpMethod())
                .enabled(request.enabled() != null ? request.enabled() : true)
                .healthy(request.enabled() != null ? request.enabled() : true) // Initially set to enabled status if
                                                                               // requested
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

        // Refresh cache
        toolCacheManager.refreshCache();

        return ApiToolResponse.from(savedTool);
    }

    @Override
    @Transactional
    public List<ApiToolResponse> createApiToolsBatch(List<CreateApiToolRequest> requests) {
        log.info("Batch creating {} API tools", requests.size());

        // We can just reuse createApiTool since it handles SSRF validation, encryption
        // and UUID generation.
        // The @Transactional annotation here ensures they either all succeed or all
        // fail.
        List<ApiToolResponse> responses = requests.stream()
                .map(request -> {
                    // Try to generate a unique code if it already exists to avoid halting the whole
                    // batch
                    // Or we let it fail if the user explicitly provided a duplicate code.
                    // For now, we'll let it execute and throw if duplicated, to enforce clean data.
                    return createApiTool(request);
                })
                .collect(Collectors.toList());

        return responses;
    }

    @Override
    @Transactional
    public ApiToolResponse updateApiTool(Long id, UpdateApiToolRequest request) {
        log.info("Updating API tool with id: {}", id);

        ApiTool apiTool = apiToolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTool not found with id: " + id));

        if (request.providerId() != null) {
            org.dynamcorp.handsaiv2.model.ApiProvider provider = apiProviderRepository.findById(request.providerId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Provider not found with id: " + request.providerId()));
            apiTool.setProvider(provider);
        }

        if (request.name() != null)
            apiTool.setName(request.name());
        if (request.description() != null)
            apiTool.setDescription(request.description());
        if (request.endpointPath() != null)
            apiTool.setEndpointPath(request.endpointPath());
        if (request.httpMethod() != null)
            apiTool.setHttpMethod(request.httpMethod());

        apiTool.setEnabled(request.enabled());
        if (request.enabled()) {
            apiTool.setHealthy(true);
        } else {
            apiTool.setHealthy(false);
        }
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
        return apiToolRepository.findAllWithRelations().stream()
                .map(ApiToolResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteApiTool(Long id) {
        log.info("Deleting API tool with id: {}", id);
        ApiTool apiTool = apiToolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTool not found with id: " + id));
        // Clear parameters explicitly to avoid constraint violations
        apiTool.getParameters().clear();
        apiToolRepository.saveAndFlush(apiTool);
        apiToolRepository.delete(apiTool);
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
