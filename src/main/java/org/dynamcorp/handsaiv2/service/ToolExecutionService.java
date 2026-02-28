package org.dynamcorp.handsaiv2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dynamcorp.handsaiv2.dto.ToolExecuteRequest;
import org.dynamcorp.handsaiv2.dto.ToolExecuteResponse;
import org.dynamcorp.handsaiv2.exception.ResourceNotFoundException;
import org.dynamcorp.handsaiv2.exception.ToolExecutionException;
import org.dynamcorp.handsaiv2.model.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
@Slf4j
@RequiredArgsConstructor
public class ToolExecutionService {

    private final ApiToolService apiToolService;
    private final ToolCacheManager toolCacheManager;
    private final LogBatchProcessor logBatchProcessor;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final EncryptionService encryptionService;
    private final org.dynamcorp.handsaiv2.util.LogObfuscator logObfuscator;
    private final DynamicTokenManager dynamicTokenManager;
    private final MemoryService memoryService;

    public ToolExecuteResponse executeApiTool(ToolExecuteRequest request) {
        log.info("Executing tool: {}", request.toolName());
        Instant startTime = Instant.now();
        ToolExecutionLog executionLog = new ToolExecutionLog();
        executionLog.setSessionId(request.sessionId());

        // --- Native Memory Tools Interceptor ---
        if (request.toolName().startsWith("handsai_")) {
            return handleNativeMemoryTool(request, startTime, executionLog);
        }

        try {
            // Intentar obtener la herramienta del caché primero
            ApiTool apiTool = toolCacheManager.getCachedTool(request.toolName())
                    .orElseGet(() -> {
                        try {
                            // Si no está en caché, buscar en la base de datos
                            return apiToolService.getApiToolByCode(request.toolName());
                        } catch (Exception e) {
                            throw new ResourceNotFoundException("Tool not found: " + request.toolName());
                        }
                    });

            // Verificar que la herramienta esté habilitada y saludable
            if (!apiTool.isEnabled() || !apiTool.isHealthy()) {
                throw new ToolExecutionException("Tool is disabled or unhealthy: " + request.toolName());
            }

            executionLog.setApiTool(apiTool);

            // Convertir los parámetros a JSON y ofuscar para el log
            String requestPayload = objectMapper.writeValueAsString(request.parameters());
            executionLog.setRequestPayload(logObfuscator.obfuscate(requestPayload));

            // Ejecutar la llamada a la API externa
            String dynamicToken = null;
            if (apiTool.getProvider().isDynamicAuth()) {
                dynamicToken = dynamicTokenManager.getToken(apiTool.getProvider());
            }

            Object result = null;
            try {
                result = executeApiCall(apiTool, request.parameters(), dynamicToken);
                if (apiTool.getProvider().isDynamicAuth() && isResultInvalid(result, apiTool.getProvider())) {
                    throw new HttpClientErrorException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                            "Invalidated by keyword");
                }
            } catch (Exception e) {
                boolean isUnauthorized = (e instanceof HttpClientErrorException
                        && ((HttpClientErrorException) e).getStatusCode().value() == 401);
                boolean isKeywordInvalid = apiTool.getProvider().isDynamicAuth()
                        && isExceptionInvalid(e, apiTool.getProvider());

                if (apiTool.getProvider().isDynamicAuth() && (isUnauthorized || isKeywordInvalid)) {
                    log.warn("Dynamic token expired or invalid for provider {}, fetching new token and retrying",
                            apiTool.getProvider().getId());
                    dynamicTokenManager.invalidateToken(apiTool.getProvider().getId());
                    dynamicToken = dynamicTokenManager.getToken(apiTool.getProvider());
                    result = executeApiCall(apiTool, request.parameters(), dynamicToken);
                    if (isResultInvalid(result, apiTool.getProvider())) {
                        throw new ToolExecutionException(
                                "Tool execution failed even after token refresh due to invalidation keywords.");
                    }
                } else {
                    throw e;
                }
            }

            // Convertir el resultado a JSON y ofuscar para el log
            String responsePayload = objectMapper.writeValueAsString(result);
            executionLog.setResponsePayload(logObfuscator.obfuscate(responsePayload));
            executionLog.setSuccess(true);

            // Calcular tiempo de ejecución
            long executionTime = Duration.between(startTime, Instant.now()).toMillis();
            executionLog.setExecutionTimeMs(executionTime);
            executionLog.setExecutedAt(Instant.now());

            // Encolar el log de ejecución para proceso por lotes
            logBatchProcessor.enqueueLog(executionLog);

            log.info("Tool execution successful: {} in {}ms", request.toolName(), executionTime);

            return new ToolExecuteResponse(
                    true,
                    result,
                    executionTime,
                    "api_tool",
                    null);

        } catch (Exception e) {
            log.error("Error executing tool {}: {}", request.toolName(), e.getMessage());

            // Registrar el error en el log
            executionLog.setSuccess(false);
            executionLog.setErrorMessage(e.getMessage());
            executionLog.setExecutedAt(Instant.now());
            executionLog.setExecutionTimeMs(
                    Duration.between(startTime, Instant.now()).toMillis());

            try {
                logBatchProcessor.enqueueLog(executionLog);
            } catch (Exception logError) {
                log.error("Failed to save execution log: {}", logError.getMessage());
            }

            return new ToolExecuteResponse(
                    false,
                    null,
                    Duration.between(startTime, Instant.now()).toMillis(),
                    "api_tool",
                    e.getMessage());
        }
    }

    private Object executeApiCall(ApiTool apiTool, Map<String, Object> parameters, String dynamicToken) {
        RestClient client = restClientBuilder.baseUrl(apiTool.getProvider().getBaseUrl()).build();

        // Preparar parámetros incluyendo autenticación
        Map<String, Object> finalParameters = prepareParametersWithAuth(apiTool, parameters, dynamicToken);
        String uriPath = buildUriWithQueryParams(apiTool, finalParameters);

        HttpMethod httpMethod = convertHttpMethod(apiTool.getHttpMethod());

        // Configurar el método HTTP y la URI
        RestClient.RequestBodySpec requestSpec = client.method(httpMethod).uri(uriPath);

        // Configurar autenticación
        configureAuthentication(requestSpec, apiTool, dynamicToken);

        // Configurar headers personalizados opcionales
        if (apiTool.getProvider().getCustomHeadersJson() != null
                && !apiTool.getProvider().getCustomHeadersJson().isEmpty()) {
            try {
                Map<String, String> customHeaders = objectMapper.readValue(apiTool.getProvider().getCustomHeadersJson(),
                        new TypeReference<Map<String, String>>() {
                        });
                customHeaders.forEach(requestSpec::header);
            } catch (Exception e) {
                log.warn("Failed to parse customHeadersJson for tool execution: {}",
                        apiTool.getProvider().getCustomHeadersJson(), e);
            }
        }

        // Configurar el body para métodos que lo requieren
        if (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE) {
            return requestSpec
                    .retrieve()
                    .body(Object.class);
        } else {
            Map<String, Object> bodyParameters = prepareBodyParameters(apiTool, finalParameters, dynamicToken);
            return requestSpec
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bodyParameters)
                    .retrieve()
                    .body(Object.class);
        }
    }

    private HttpMethod convertHttpMethod(HttpMethodEnum methodEnum) {
        switch (methodEnum) {
            case GET:
                return HttpMethod.GET;
            case POST:
                return HttpMethod.POST;
            case PUT:
                return HttpMethod.PUT;
            case DELETE:
                return HttpMethod.DELETE;
            case PATCH:
                return HttpMethod.PATCH;
            default:
                throw new ToolExecutionException("Unsupported HTTP method: " + methodEnum);
        }
    }

    private Map<String, Object> prepareParametersWithAuth(ApiTool apiTool, Map<String, Object> originalParameters,
            String dynamicToken) {
        Map<String, Object> parameters = new java.util.HashMap<>(originalParameters);

        // Añadir API key como parámetro si la localización es QUERY_PARAM
        if (apiTool.getProvider().getAuthenticationType() == AuthenticationTypeEnum.API_KEY &&
                apiTool.getProvider().getApiKeyLocation() == ApiKeyLocationEnum.QUERY_PARAMETER &&
                apiTool.getProvider().getApiKeyName() != null) {

            String token = getEffectiveToken(apiTool.getProvider(), dynamicToken);
            if (token != null) {
                parameters.put(apiTool.getProvider().getApiKeyName(), token);
            }
        }

        return parameters;
    }

    private String buildUriWithQueryParams(ApiTool apiTool, Map<String, Object> parameters) {
        String basePath = apiTool.getEndpointPath();

        // Sustituir path parameters del tipo {paramName} con sus valores
        Map<String, Object> remainingParams = new java.util.HashMap<>(parameters);
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\{([^}]+)}")
                .matcher(basePath);
        StringBuffer resolvedPath = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = remainingParams.remove(paramName);
            matcher.appendReplacement(resolvedPath, value != null ? value.toString() : matcher.group(0));
        }
        matcher.appendTail(resolvedPath);
        basePath = resolvedPath.toString();

        // Si hay parámetros configurados para ir en la URL (como la API Key de tipo
        // Query Param),
        // o si es GET/DELETE donde todos van a la URL, los agregamos
        boolean shouldAppendParams = !remainingParams.isEmpty() &&
                (apiTool.getHttpMethod() == HttpMethodEnum.GET ||
                        apiTool.getHttpMethod() == HttpMethodEnum.DELETE ||
                        hasQueryParametersForNonGet(apiTool, remainingParams));

        if (shouldAppendParams) {
            StringBuilder uriBuilder = new StringBuilder(basePath);
            // Verificar si el basePath ya contiene parámetros query
            if (basePath.contains("?")) {
                if (!basePath.endsWith("?") && !basePath.endsWith("&")) {
                    uriBuilder.append("&");
                }
            } else {
                uriBuilder.append("?");
            }

            remainingParams.entrySet().forEach(entry -> {
                uriBuilder.append(entry.getKey())
                        .append("=")
                        .append(entry.getValue())
                        .append("&");
            });

            // Remover el último &
            if (uriBuilder.charAt(uriBuilder.length() - 1) == '&') {
                uriBuilder.setLength(uriBuilder.length() - 1);
            }

            return uriBuilder.toString();
        }

        return basePath;
    }

    private boolean hasQueryParametersForNonGet(ApiTool apiTool, Map<String, Object> parameters) {
        // En POST/PUT/PATCH, el único parámetro que debería ir por Query String
        // (por el momento) es la API Key, si así está configurado el provider.
        return apiTool.getProvider().getAuthenticationType() == AuthenticationTypeEnum.API_KEY &&
                apiTool.getProvider().getApiKeyLocation() == ApiKeyLocationEnum.QUERY_PARAMETER &&
                apiTool.getProvider().getApiKeyName() != null &&
                parameters.containsKey(apiTool.getProvider().getApiKeyName());
    }

    private void configureAuthentication(RestClient.RequestBodySpec requestSpec, ApiTool apiTool, String dynamicToken) {
        if (apiTool.getProvider().getAuthenticationType() == null) {
            return;
        }

        String token = getEffectiveToken(apiTool.getProvider(), dynamicToken);
        if (token == null)
            return;

        switch (apiTool.getProvider().getAuthenticationType()) {
            case API_KEY:
                if (apiTool.getProvider().getApiKeyLocation() == ApiKeyLocationEnum.HEADER) {
                    String headerName = apiTool.getProvider().getApiKeyName() != null
                            ? apiTool.getProvider().getApiKeyName()
                            : "X-API-Key";
                    requestSpec.header(headerName, token);
                } else if (apiTool.getProvider().getApiKeyLocation() == ApiKeyLocationEnum.QUERY_PARAMETER) {
                    // La lógica para añadir la API Key como Query Parameter ahora se maneja en
                    // buildUriWithQueryParams y prepareParametersWithAuth para todos los verbos
                    // HTTP.
                }
                break;

            case BEARER_TOKEN:
                requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                break;

            case BASIC_AUTH:
                requestSpec.header("Authorization", "Basic " + token);
                break;

            default:
                log.warn("Unsupported authentication type: {}", apiTool.getProvider().getAuthenticationType());
                break;
        }
    }

    private Map<String, Object> prepareBodyParameters(ApiTool apiTool, Map<String, Object> parameters,
            String dynamicToken) {
        Map<String, Object> bodyParams = new java.util.HashMap<>(parameters);

        // Remover parámetros de path ({paramName}) del body
        java.util.regex.Matcher pathMatcher = java.util.regex.Pattern
                .compile("\\{([^}]+)}")
                .matcher(apiTool.getEndpointPath());
        while (pathMatcher.find()) {
            bodyParams.remove(pathMatcher.group(1));
        }

        // Remover parámetros de query si están en el body
        if (apiTool.getProvider().getAuthenticationType() == AuthenticationTypeEnum.API_KEY &&
                apiTool.getProvider().getApiKeyLocation() == ApiKeyLocationEnum.QUERY_PARAMETER &&
                apiTool.getProvider().getApiKeyName() != null) {

            // Para POST/PUT/PATCH, remover la API key del body si está configurada como
            // query param
            // ya que se maneja en la URL
            bodyParams.remove(apiTool.getProvider().getApiKeyName());
        }

        // Inyectar API key en el body si la localización es IN_BODY
        if (apiTool.getProvider().getAuthenticationType() == AuthenticationTypeEnum.API_KEY &&
                apiTool.getProvider().getApiKeyLocation() == ApiKeyLocationEnum.IN_BODY &&
                apiTool.getProvider().getApiKeyName() != null) {

            String token = getEffectiveToken(apiTool.getProvider(), dynamicToken);
            if (token != null) {
                // Siempre forzamos el put para que pise cualquier valor dummy enviado
                bodyParams.put(apiTool.getProvider().getApiKeyName(), token);
            }
        }

        return bodyParams;
    }

    private boolean isResultInvalid(Object result, ApiProvider provider) {
        if (result == null || provider.getDynamicAuthInvalidationKeywords() == null
                || provider.getDynamicAuthInvalidationKeywords().isBlank())
            return false;
        String responseStr = result.toString().toLowerCase();
        String[] keywords = provider.getDynamicAuthInvalidationKeywords().toLowerCase().split(",");
        for (String keyword : keywords) {
            if (!keyword.trim().isEmpty() && responseStr.contains(keyword.trim()))
                return true;
        }
        return false;
    }

    private boolean isExceptionInvalid(Exception e, ApiProvider provider) {
        if (e.getMessage() == null || provider.getDynamicAuthInvalidationKeywords() == null
                || provider.getDynamicAuthInvalidationKeywords().isBlank())
            return false;
        String errorStr = e.getMessage().toLowerCase();
        if (e instanceof HttpClientErrorException) {
            errorStr += " " + ((HttpClientErrorException) e).getResponseBodyAsString().toLowerCase();
        }
        String[] keywords = provider.getDynamicAuthInvalidationKeywords().toLowerCase().split(",");
        for (String keyword : keywords) {
            if (!keyword.trim().isEmpty() && errorStr.contains(keyword.trim()))
                return true;
        }
        return false;
    }

    private String getEffectiveToken(ApiProvider provider, String dynamicToken) {
        if (dynamicToken != null)
            return dynamicToken;
        if (provider.getApiKeyValue() != null)
            return encryptionService.decrypt(provider.getApiKeyValue());
        return null;
    }

    private ToolExecuteResponse handleNativeMemoryTool(ToolExecuteRequest request, java.time.Instant startTime,
            ToolExecutionLog executionLog) {
        log.info("Intercepted native memory tool execution: {}", request.toolName());
        try {
            java.util.Map<String, Object> params = request.parameters();
            Object resObj = null;

            switch (request.toolName()) {
                case "handsai_save_intent":
                    resObj = memoryService.saveIntent(
                            getStringParam(params, "agent_id"),
                            getStringParam(params, "session_id"),
                            getStringParam(params, "intent"),
                            getStringParam(params, "verified"),
                            getDoubleParam(params, "confidence"),
                            getStringParam(params, "boundary_hit"),
                            getStringParam(params, "tags"));
                    break;
                case "handsai_get_intent":
                    resObj = memoryService.getActiveIntents(
                            getStringParam(params, "agent_id"),
                            getStringParam(params, "tags"));
                    break;
                case "handsai_complete_intent":
                    resObj = memoryService.completeIntent(getLongParam(params, "id")).orElse(null);
                    break;
                case "handsai_delete_intent":
                    memoryService.deleteIntent(getLongParam(params, "id"));
                    resObj = "Intent deleted successfully";
                    break;
                case "handsai_save_knowledge":
                    resObj = memoryService.saveKnowledge(
                            getStringParam(params, "title"),
                            org.dynamcorp.handsaiv2.model.KnowledgeCategoryEnum
                                    .valueOf(getStringParam(params, "category").toUpperCase()),
                            getStringParam(params, "content_what"),
                            getStringParam(params, "content_why"),
                            getStringParam(params, "content_where"),
                            getStringParam(params, "content_learned"));
                    break;
                case "handsai_search_knowledge":
                    resObj = memoryService.searchKnowledge(
                            getStringParam(params, "query"),
                            getStringParam(params, "category"));
                    break;
                case "handsai_delete_knowledge":
                    memoryService.deleteKnowledge(getLongParam(params, "id"));
                    resObj = "Knowledge deleted successfully";
                    break;
                default:
                    throw new IllegalArgumentException("Unknown native tool: " + request.toolName());
            }

            Object result = objectMapper.writeValueAsString(resObj);

            long executionTime = java.time.Duration.between(startTime, java.time.Instant.now()).toMillis();

            // Build pseudo log for analytics
            executionLog.setSuccess(true);
            executionLog.setExecutionTimeMs(executionTime);
            executionLog.setRequestPayload(objectMapper.writeValueAsString(params));
            executionLog.setResponsePayload(objectMapper.writeValueAsString(result));
            executionLog.setExecutedAt(java.time.Instant.now());
            logBatchProcessor.enqueueLog(executionLog);

            return new ToolExecuteResponse(true, result, executionTime, "system_tool", null);

        } catch (Exception e) {
            log.error("Error executing native memory tool {}", request.toolName(), e);
            long executionTime = java.time.Duration.between(startTime, java.time.Instant.now()).toMillis();

            executionLog.setSuccess(false);
            executionLog.setErrorMessage(e.getMessage());
            executionLog.setExecutionTimeMs(executionTime);
            executionLog.setExecutedAt(java.time.Instant.now());
            logBatchProcessor.enqueueLog(executionLog);

            return new ToolExecuteResponse(false, null, executionTime, "system_tool", e.getMessage());
        }
    }

    private String getStringParam(java.util.Map<String, Object> params, String key) {
        Object val = params.get(key);
        return val != null ? val.toString() : null;
    }

    private Double getDoubleParam(java.util.Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val instanceof Number)
            return ((Number) val).doubleValue();
        if (val instanceof String)
            return Double.parseDouble((String) val);
        return 0.0;
    }

    private Long getLongParam(java.util.Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val instanceof Number)
            return ((Number) val).longValue();
        if (val instanceof String)
            return Long.parseLong((String) val);
        if (val == null)
            throw new IllegalArgumentException("Missing required numeric parameter: " + key);
        return 0L;
    }
}
