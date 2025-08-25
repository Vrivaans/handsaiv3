package org.dynamcorp.handsaiv2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dynamcorp.handsaiv2.dto.ToolExecuteRequest;
import org.dynamcorp.handsaiv2.dto.ToolExecuteResponse;
import org.dynamcorp.handsaiv2.exception.ResourceNotFoundException;
import org.dynamcorp.handsaiv2.exception.ToolExecutionException;
import org.dynamcorp.handsaiv2.model.ApiTool;
import org.dynamcorp.handsaiv2.model.HttpMethodEnum;
import org.dynamcorp.handsaiv2.model.ToolExecutionLog;
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

        // Configurar el método HTTP y los parámetros según la definición de la herramienta
        WebClient.RequestBodySpec requestSpec;
        if (HttpMethodEnum.GET.equals(apiTool.getHttpMethod())) {
            requestSpec = (WebClient.RequestBodySpec) client.get().uri(apiTool.getEndpointPath());
        } else if (HttpMethodEnum.POST.equals(apiTool.getHttpMethod())) {
            requestSpec = client.post().uri(apiTool.getEndpointPath());
        } else if (HttpMethodEnum.PUT.equals(apiTool.getHttpMethod())) {
            requestSpec = client.put().uri(apiTool.getEndpointPath());
        } else if (HttpMethodEnum.DELETE.equals(apiTool.getHttpMethod())) {
            requestSpec = (WebClient.RequestBodySpec) client.delete().uri(apiTool.getEndpointPath());
        } else if (HttpMethodEnum.PATCH.equals(apiTool.getHttpMethod())) {
            requestSpec = client.patch().uri(apiTool.getEndpointPath());
        } else {
            throw new ToolExecutionException("Unsupported HTTP method: " + apiTool.getHttpMethod());
        }

        // Configurar el body para métodos que lo requieren
        Mono<Object> responseMono;
        if (apiTool.getHttpMethod() == HttpMethodEnum.GET ||
            apiTool.getHttpMethod() == HttpMethodEnum.DELETE) {
            responseMono = requestSpec
                    .retrieve()
                    .bodyToMono(Object.class);
        } else {
            responseMono = requestSpec
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(parameters))
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
}
