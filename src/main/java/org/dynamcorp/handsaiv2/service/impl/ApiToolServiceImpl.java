package org.dynamcorp.handsaiv2.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dynamcorp.handsaiv2.dto.ApiToolResponse;
import org.dynamcorp.handsaiv2.dto.CreateApiToolRequest;
import org.dynamcorp.handsaiv2.dto.ToolParameterRequest;
import org.dynamcorp.handsaiv2.dto.UpdateApiToolRequest;
import org.dynamcorp.handsaiv2.exception.ResourceNotFoundException;
import org.dynamcorp.handsaiv2.model.ApiTool;
import org.dynamcorp.handsaiv2.model.AuthenticationTypeEnum;
import org.dynamcorp.handsaiv2.model.ToolParameter;
import org.dynamcorp.handsaiv2.repository.ApiToolRepository;
import org.dynamcorp.handsaiv2.service.ApiToolService;
import org.dynamcorp.handsaiv2.service.ToolCacheManager;
import org.dynamcorp.handsaiv2.service.ToolValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiToolServiceImpl implements ApiToolService {

    private final ApiToolRepository apiToolRepository;
    private final ToolValidationService toolValidationService;
    private final WebClient.Builder webClientBuilder;
    private final Executor taskExecutor;
    private final ToolCacheManager toolCacheManager;

    @Override
    @Transactional
    public CompletableFuture<ApiToolResponse> createApiTool(CreateApiToolRequest request) {
        return CompletableFuture.supplyAsync(() -> {

            validateData(request);

            // Crear la entidad ApiTool
            ApiTool apiTool = ApiTool.builder()
                    .name(request.name())
                    .description(request.description())
                    .baseUrl(request.baseUrl())
                    .endpointPath(request.endpointPath())
                    .httpMethod(request.httpMethod())
                    .authenticationType(request.authenticationType())
                    .apiKeyLocation(request.apiKeyLocation())
                    .apiKeyName(request.apiKeyName())
                    .apiKeyValue(request.apiKeyValue()) // TODO: Hash this value before saving
                    .code(request.code())
                    .enabled(request.enabled())
                    .healthy(false) // Inicialmente no verificada
                    .build();

            // Guardar la entidad para obtener su ID
            ApiTool savedTool = apiToolRepository.save(apiTool);

            // Crear y asignar los parámetros
            List<ToolParameter> parameters = request.parameters().stream()
                    .map(paramRequest -> createToolParameter(paramRequest, savedTool))
                    .collect(Collectors.toList());

            savedTool.setParameters(parameters);

            // Validar la salud de la herramienta
            CompletableFuture.runAsync(() -> {
                validateHealth(savedTool);
            }, taskExecutor);
            toolCacheManager.addOrUpdateTool(savedTool);

            return ApiToolResponse.from(savedTool);
        }, taskExecutor);
    }

    public void validateData(CreateApiToolRequest request){
        if(request.name() == null || request.name().isBlank()){
            throw new IllegalArgumentException("ApiTool name is required");
        }
        if(request.description() == null || request.description().isBlank()){
            throw new IllegalArgumentException("ApiTool description is required");
        }if(request.enabled() == null){
            throw new IllegalArgumentException("ApiTool enabled is required");
        }
        if(request.baseUrl() == null || request.baseUrl().isBlank()){
            throw new IllegalArgumentException("ApiTool baseUrl is required");
        }
        if(request.endpointPath() == null || request.endpointPath().isBlank()){
            throw new IllegalArgumentException("ApiTool endpointPath is required");
        }
        if(request.httpMethod() == null){
            throw new IllegalArgumentException("ApiTool httpMethod is required");
        }
        if(request.authenticationType() == null){
            throw new IllegalArgumentException("ApiTool authenticationType is required");
        }
        if(request.apiKeyLocation() == null){
            throw new IllegalArgumentException("ApiTool apiKeyLocation is required");
        }

        // Solo validar apiKeyName y apiKeyValue si la autenticación no es NONE
        if(request.authenticationType() != AuthenticationTypeEnum.NONE) {
            if(request.apiKeyName() == null || request.apiKeyName().isBlank()){
                throw new IllegalArgumentException("ApiTool apiKeyName is required when authentication is not NONE");
            }
            if(request.apiKeyValue() == null || request.apiKeyValue().isBlank()){
                throw new IllegalArgumentException("ApiTool apiKeyValue is required when authentication is not NONE");
            }
        }

        if(request.code() == null || request.code().isBlank()){
            throw new IllegalArgumentException("ApiTool code is required");
        }
        if(request.parameters() == null || request.parameters().isEmpty()){
            throw new IllegalArgumentException("ApiTool parameters is required");
        }
        validateParametersData(request);
    }

    public void validateParametersData(CreateApiToolRequest request){
        if(request.parameters() != null || !request.parameters().isEmpty()){
            request.parameters().forEach(paramRequest -> {
                if(paramRequest.name() == null || paramRequest.name().isBlank()){
                    throw new IllegalArgumentException("ApiTool parameter name is required");
                }
                if(paramRequest.type() == null){
                    throw new IllegalArgumentException("ApiTool parameter type is required");
                }
                if(paramRequest.description() == null || paramRequest.description().isBlank()){
                    throw new IllegalArgumentException("ApiTool parameter description is required");
                }
                if(paramRequest.required() == null){
                    throw new IllegalArgumentException("ApiTool parameter required is required");
                }
            });
        }
    }

    @Override
    @Transactional
    public CompletableFuture<ApiToolResponse> updateApiTool(Long id, UpdateApiToolRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ApiTool apiTool = apiToolRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("ApiTool not found with id: " + id));

            apiTool.setName(request.name());
            apiTool.setDescription(request.description());
            apiTool.setBaseUrl(request.baseUrl());
            apiTool.setEndpointPath(request.endpointPath());
            apiTool.setHttpMethod(request.httpMethod());
            apiTool.setEnabled(request.enabled());
            apiTool.setAuthenticationType(request.authenticationType());
            apiTool.setApiKeyLocation(request.apiKeyLocation());
            apiTool.setApiKeyName(request.apiKeyName());

            // The API key value is optional during updates.
            // Only update it if a new non-blank value is provided.
            if (request.apiKeyValue() != null && !request.apiKeyValue().isBlank()) {
                apiTool.setApiKeyValue(request.apiKeyValue()); // TODO: Hash this value before saving
            }


            // Limpiar parámetros existentes y agregar los nuevos
            apiTool.getParameters().clear();

            List<ToolParameter> updatedParameters = request.parameters().stream()
                    .map(paramRequest -> createToolParameter(paramRequest, apiTool))
                    .collect(Collectors.toList());

            apiTool.getParameters().addAll(updatedParameters);

            // Validar la salud de la herramienta
            CompletableFuture.runAsync(() -> {
                validateHealth(apiTool);
            }, taskExecutor);

            return ApiToolResponse.from(apiToolRepository.save(apiTool));
        }, taskExecutor);
    }

    @Override
    public CompletableFuture<ApiToolResponse> getApiTool(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            ApiTool apiTool = apiToolRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("ApiTool not found with id: " + id));

            return ApiToolResponse.from(apiTool);
        }, taskExecutor);
    }

    @Override
    public CompletableFuture<List<ApiToolResponse>> getAllApiTools() {
        return CompletableFuture.supplyAsync(() -> {
            return apiToolRepository.findAll().stream()
                    .map(ApiToolResponse::from)
                    .collect(Collectors.toList());
        }, taskExecutor);
    }

    @Override
    @Transactional
    public CompletableFuture<Void> deleteApiTool(Long id) {
        return CompletableFuture.runAsync(() -> {
            if (!apiToolRepository.existsById(id)) {
                throw new ResourceNotFoundException("ApiTool not found with id: " + id);
            }

            apiToolRepository.deleteById(id);
        }, taskExecutor);
    }

    @Override
    @Transactional
    public CompletableFuture<ApiToolResponse> validateApiToolHealth(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            ApiTool apiTool = apiToolRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("ApiTool not found with id: " + id));

            validateHealth(apiTool);

            return ApiToolResponse.from(apiTool);
        }, taskExecutor);
    }

    @Override
    public CompletableFuture<ApiTool> getApiToolByCode(String code) {
        return CompletableFuture.supplyAsync(() -> {
            return apiToolRepository.findByCode(code)
                    .orElseThrow(() -> new ResourceNotFoundException("ApiTool not found with code: " + code));
        }, taskExecutor);
    }

    private ToolParameter createToolParameter(ToolParameterRequest request, ApiTool apiTool) {
        return ToolParameter.builder()
                .name(request.name())
                .type(request.type())
                .description(request.description())
                .required(request.required())
                .defaultValue(request.defaultValue())
                .apiTool(apiTool)
                .code(UUID.randomUUID().toString().substring(0, 8))
                .build();
    }

    private void validateHealth(ApiTool apiTool) {
        toolValidationService.validateApiToolHealth(apiTool)
            .thenAccept(isHealthy -> {
                apiTool.setHealthy(isHealthy);
                apiTool.setLastHealthCheck(Instant.now());
                apiToolRepository.save(apiTool);
            })
            .exceptionally(throwable -> {
                log.error("Error during health validation for tool {}: {}", 
                    apiTool.getName(), throwable.getMessage());
                apiTool.setHealthy(false);
                apiTool.setLastHealthCheck(Instant.now());
                apiToolRepository.save(apiTool);
                return null;
            });
    }
}
