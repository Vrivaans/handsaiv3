package org.dynamcorp.handsaiv2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dynamcorp.handsaiv2.model.ApiTool;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
@RequiredArgsConstructor
public class ToolValidationService {

    private final WebClient.Builder webClientBuilder;
    private final Executor taskExecutor;

    public CompletableFuture<Boolean> validateApiToolHealth(ApiTool apiTool) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = apiTool.getBaseUrl();

                // Simple ping check to validate if the API is accessible
                WebClient webClient = webClientBuilder.baseUrl(url).build();

                // Hacemos un HEAD request para validar que el endpoint responde
                return webClient.head()
                        .retrieve()
                        .toBodilessEntity()
                        .timeout(Duration.ofSeconds(5))
                        .map(response -> {
                            log.info("API tool health check successful for {}", apiTool.getName());
                            return true;
                        })
                        .onErrorResume(e -> {
                            log.warn("API tool health check failed for {}: {}", apiTool.getName(), e.getMessage());
                            return Mono.just(false);
                        })
                        .toFuture()
                        .join(); // Safe to use join() inside virtual thread

            } catch (Exception e) {
                log.error("Error validating API tool {}: {}", apiTool.getName(), e.getMessage());
                return false;
            }
        }, taskExecutor);
    }
}
