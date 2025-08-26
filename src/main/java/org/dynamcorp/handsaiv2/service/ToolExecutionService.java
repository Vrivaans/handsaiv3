package org.dynamcorp.handsaiv2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dynamcorp.handsaiv2.dto.ToolExecuteRequest;
import org.dynamcorp.handsaiv2.dto.ToolExecuteResponse;
import org.dynamcorp.handsaiv2.exception.ResourceNotFoundException;
import org.dynamcorp.handsaiv2.exception.ToolExecutionException;
import org.dynamcorp.handsaiv2.model.*;
import org.dynamcorp.handsaiv2.repository.ToolExecutionLogRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
@RequiredArgsConstructor
public class ToolExecutionService {

    private final ApiToolService apiToolService;
    private final ToolCacheManager toolCacheManager;
    private final ToolExecutionLogRepository logRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final Executor taskExecutor;

    public CompletableFuture<ToolExecuteResponse> executeApiTool(ToolExecuteRequest request) {
        return CompletableFuture.supplyAsync(() -> {
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
                                return apiToolService.getApiToolByCode(request.toolName()).join();
                            } catch (Exception e) {
                                throw new ResourceNotFoundException("Tool not found: " + request.toolName());
                            }
                        });

                // Verificar que la herramienta esté habilitada y saludable
                if (!apiTool.isEnabled() || !apiTool.isHealthy()) {
                    throw new ToolExecutionException("Tool is disabled or unhealthy: " + request.toolName());
                }

                executionLog.setApiTool(apiTool);

                // Convertir los parámetros a JSON para el log
                String requestPayload = objectMapper.writeValueAsString(request.parameters());
                executionLog.setRequestPayload(requestPayload);

                // Ejecutar la llamada a la API externa
                Object result = executeApiCall(apiTool, request.parameters());

                // Convertir el resultado a JSON para el log
                String responsePayload = objectMapper.writeValueAsString(result);
                executionLog.setResponsePayload(responsePayload);
                executionLog.setSuccess(true);

                // Calcular tiempo de ejecución
                long executionTime = Duration.between(startTime, Instant.now()).toMillis();
                executionLog.setExecutionTimeMs(executionTime);
                executionLog.setExecutedAt(Instant.now());

                // Guardar el log de ejecución
                logRepository.save(executionLog);

                log.info("Tool execution successful: {} in {}ms", request.toolName(), executionTime);

                return new ToolExecuteResponse(
                        true,
                        result,
                        executionTime,
                        "api_tool",
                        null
                );

            } catch (Exception e) {
                log.error("Error executing tool {}: {}", request.toolName(), e.getMessage());

                // Registrar el error en el log
                executionLog.setSuccess(false);
                executionLog.setErrorMessage(e.getMessage());
                executionLog.setExecutedAt(Instant.now());
                executionLog.setExecutionTimeMs(
                        Duration.between(startTime, Instant.now()).toMillis());

                try {
                    logRepository.save(executionLog);
                } catch (Exception logError) {
                    log.error("Failed to save execution log: {}", logError.getMessage());
                }

                return new ToolExecuteResponse(
                        false,
                        null,
                        Duration.between(startTime, Instant.now()).toMillis(),
                        "api_tool",
                        e.getMessage()
                );
            }
        }, taskExecutor);
    }

    private Object executeApiCall(ApiTool apiTool, Map<String, Object> parameters) {
        WebClient client = webClientBuilder.baseUrl(apiTool.getBaseUrl()).build();

        // Preparar parámetros incluyendo autenticación
        Map<String, Object> finalParameters = prepareParametersWithAuth(apiTool, parameters);
        String uriPath = buildUriWithQueryParams(apiTool, finalParameters);

        // Configurar el método HTTP y los parámetros según la definición de la herramienta
        WebClient.RequestBodySpec requestSpec;
        if (HttpMethodEnum.GET.equals(apiTool.getHttpMethod())) {
            requestSpec = (WebClient.RequestBodySpec) client.get().uri(uriPath);
        } else if (HttpMethodEnum.POST.equals(apiTool.getHttpMethod())) {
            requestSpec = client.post().uri(uriPath);
        } else if (HttpMethodEnum.PUT.equals(apiTool.getHttpMethod())) {
            requestSpec = client.put().uri(uriPath);
        } else if (HttpMethodEnum.DELETE.equals(apiTool.getHttpMethod())) {
            requestSpec = (WebClient.RequestBodySpec) client.delete().uri(uriPath);
        } else if (HttpMethodEnum.PATCH.equals(apiTool.getHttpMethod())) {
            requestSpec = client.patch().uri(uriPath);
        } else {
            throw new ToolExecutionException("Unsupported HTTP method: " + apiTool.getHttpMethod());
        }

        // Configurar headers de autenticación
        requestSpec = configureAuthenticationHeaders(apiTool, requestSpec);

        // Configurar el body para métodos que lo requieren
        Mono<Object> responseMono;
        if (apiTool.getHttpMethod() == HttpMethodEnum.GET ||
            apiTool.getHttpMethod() == HttpMethodEnum.DELETE) {
            responseMono = requestSpec
                    .retrieve()
                    .bodyToMono(Object.class);
        } else {
            Map<String, Object> bodyParameters = prepareBodyParameters(apiTool, finalParameters);
            responseMono = requestSpec
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(bodyParameters))
                    .retrieve()
                    .bodyToMono(Object.class);
        }

        // Configurar timeout y reintentos
        return responseMono
                .timeout(Duration.ofSeconds(30))
                .retry(3)
                .toFuture()
                .join(); // Aquí estamos usando join() pero dentro de un CompletableFuture
    }

    private Map<String, Object> prepareParametersWithAuth(ApiTool apiTool, Map<String, Object> originalParameters) {
        Map<String, Object> parameters = new java.util.HashMap<>(originalParameters);

        // Añadir API key como parámetro si la localización es QUERY_PARAM
        if (apiTool.getAuthenticationType() == AuthenticationTypeEnum.API_KEY &&
            apiTool.getApiKeyLocation() == ApiKeyLocationEnum.QUERY_PARAMETER &&
            apiTool.getApiKeyName() != null &&
            apiTool.getApiKeyValue() != null) {

            parameters.put(apiTool.getApiKeyName(), apiTool.getApiKeyValue());
        }

        return parameters;
    }

    private String buildUriWithQueryParams(ApiTool apiTool, Map<String, Object> parameters) {
        String basePath = apiTool.getEndpointPath();

        // Para métodos GET y DELETE, añadir parámetros como query parameters
        if (apiTool.getHttpMethod() == HttpMethodEnum.GET ||
            apiTool.getHttpMethod() == HttpMethodEnum.DELETE) {

            if (!parameters.isEmpty()) {
                StringBuilder uriBuilder = new StringBuilder(basePath);
                uriBuilder.append("?");

                parameters.entrySet().forEach(entry -> {
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
        }

        return basePath;
    }

    private WebClient.RequestBodySpec configureAuthenticationHeaders(ApiTool apiTool, WebClient.RequestBodySpec requestSpec) {
        if (apiTool.getAuthenticationType() == null || apiTool.getApiKeyValue() == null) {
            return requestSpec;
        }

        switch (apiTool.getAuthenticationType()) {
            case API_KEY:
                if (apiTool.getApiKeyLocation() == ApiKeyLocationEnum.HEADER) {
                    String headerName = apiTool.getApiKeyName() != null ? 
                                      apiTool.getApiKeyName() : "X-API-Key";
                    requestSpec = requestSpec.header(headerName, apiTool.getApiKeyValue());
                }
                break;

            case BEARER_TOKEN:
                requestSpec = requestSpec.header("Authorization", "Bearer " + apiTool.getApiKeyValue());
                break;

            case BASIC_AUTH:
                requestSpec = requestSpec.header("Authorization", "Basic " + apiTool.getApiKeyValue());
                break;

            default:
                log.warn("Unsupported authentication type: {}", apiTool.getAuthenticationType());
                break;
        }

        return requestSpec;
    }

    private Map<String, Object> prepareBodyParameters(ApiTool apiTool, Map<String, Object> parameters) {
        Map<String, Object> bodyParams = new java.util.HashMap<>(parameters);

        // Remover parámetros de query si están en el body
        if (apiTool.getAuthenticationType() == AuthenticationTypeEnum.API_KEY &&
            apiTool.getApiKeyLocation() == ApiKeyLocationEnum.QUERY_PARAMETER &&
            apiTool.getApiKeyName() != null) {

            // Para POST/PUT/PATCH, remover la API key del body si está configurada como query param
            // ya que se maneja en la URL
            bodyParams.remove(apiTool.getApiKeyName());
        }

        return bodyParams;
    }
}
