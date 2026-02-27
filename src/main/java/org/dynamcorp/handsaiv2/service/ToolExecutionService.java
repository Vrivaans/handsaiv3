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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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

    public ToolExecuteResponse executeApiTool(ToolExecuteRequest request) {
        log.info("Executing tool: {}", request.toolName());
        Instant startTime = Instant.now();
        ToolExecutionLog executionLog = new ToolExecutionLog();
        executionLog.setSessionId(request.sessionId());

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
            Object result = executeApiCall(apiTool, request.parameters());

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

    private Object executeApiCall(ApiTool apiTool, Map<String, Object> parameters) {
        RestClient client = restClientBuilder.baseUrl(apiTool.getProvider().getBaseUrl()).build();

        // Preparar parámetros incluyendo autenticación
        Map<String, Object> finalParameters = prepareParametersWithAuth(apiTool, parameters);
        String uriPath = buildUriWithQueryParams(apiTool, finalParameters);

        HttpMethod httpMethod = convertHttpMethod(apiTool.getHttpMethod());

        // Configurar el método HTTP y la URI
        RestClient.RequestBodySpec requestSpec = client.method(httpMethod).uri(uriPath);

        // Configurar autenticación
        configureAuthentication(requestSpec, apiTool);

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
            Map<String, Object> bodyParameters = prepareBodyParameters(apiTool, finalParameters);
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

    private Map<String, Object> prepareParametersWithAuth(ApiTool apiTool, Map<String, Object> originalParameters) {
        Map<String, Object> parameters = new java.util.HashMap<>(originalParameters);

        // Añadir API key como parámetro si la localización es QUERY_PARAM
        if (apiTool.getProvider().getAuthenticationType() == AuthenticationTypeEnum.API_KEY &&
                apiTool.getProvider().getApiKeyLocation() == ApiKeyLocationEnum.QUERY_PARAMETER &&
                apiTool.getProvider().getApiKeyName() != null &&
                apiTool.getProvider().getApiKeyValue() != null) {

            parameters.put(apiTool.getProvider().getApiKeyName(),
                    encryptionService.decrypt(apiTool.getProvider().getApiKeyValue()));
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

    private void configureAuthentication(RestClient.RequestBodySpec requestSpec, ApiTool apiTool) {
        if (apiTool.getProvider().getAuthenticationType() == null || apiTool.getProvider().getApiKeyValue() == null) {
            return;
        }

        switch (apiTool.getProvider().getAuthenticationType()) {
            case API_KEY:
                String apiKey = encryptionService.decrypt(apiTool.getProvider().getApiKeyValue());
                if (apiTool.getProvider().getApiKeyLocation() == ApiKeyLocationEnum.HEADER) {
                    String headerName = apiTool.getProvider().getApiKeyName() != null
                            ? apiTool.getProvider().getApiKeyName()
                            : "X-API-Key";
                    requestSpec.header(headerName, apiKey);
                } else if (apiTool.getProvider().getApiKeyLocation() == ApiKeyLocationEnum.QUERY_PARAMETER) {
                    // La lógica para añadir la API Key como Query Parameter ahora se maneja en
                    // buildUriWithQueryParams y prepareParametersWithAuth para todos los verbos
                    // HTTP.
                }
                break;

            case BEARER_TOKEN:
                String token = encryptionService.decrypt(apiTool.getProvider().getApiKeyValue());
                requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                break;

            case BASIC_AUTH:
                requestSpec.header("Authorization",
                        "Basic " + encryptionService.decrypt(apiTool.getProvider().getApiKeyValue()));
                break;

            default:
                log.warn("Unsupported authentication type: {}", apiTool.getProvider().getAuthenticationType());
                break;
        }
    }

    private Map<String, Object> prepareBodyParameters(ApiTool apiTool, Map<String, Object> parameters) {
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
                apiTool.getProvider().getApiKeyName() != null &&
                apiTool.getProvider().getApiKeyValue() != null) {

            // Siempre forzamos el put para que pise cualquier valor dummy
            // enviado desde el payload del servidor MCP.
            bodyParams.put(apiTool.getProvider().getApiKeyName(),
                    encryptionService.decrypt(apiTool.getProvider().getApiKeyValue()));
        }

        return bodyParams;
    }
}
