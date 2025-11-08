package org.dynamcorp.handsaiv2.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.dynamcorp.handsaiv2.model.ApiTool;
import org.dynamcorp.handsaiv2.repository.ApiToolRepository;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class ToolCacheManager {

    private final ApiToolRepository apiToolRepository;

    private final ConcurrentHashMap<String, ApiTool> toolCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initCache() {
        log.info("Initializing tool cache");
        List<ApiTool> activeTools = apiToolRepository.findAllEnabled();
        activeTools.forEach(tool -> toolCache.put(tool.getCode(), tool));
        log.info("Tool cache initialized with {} tools", activeTools.size());
    }

    public List<ApiTool> getAllCachedTools() {
        return toolCache.values().stream().toList();
    }

    /**
     * Versión reactiva para obtener todas las herramientas cacheadas usando Flux
     * Permite procesamiento no bloqueante y backpressure handling
     */
    public Flux<ApiTool> getAllCachedToolsReactive() {
        return Flux.fromIterable(toolCache.values())
                .filter(tool -> tool.isEnabled() && tool.isHealthy())
                .doOnSubscribe(subscription -> log.debug("Starting reactive tool cache retrieval"))
                .doOnComplete(() -> log.debug("Completed reactive tool cache retrieval"));
    }

    /**
     * Versión reactiva para obtener una herramienta específica por código
     */
    public Mono<ApiTool> getCachedToolReactive(String toolCode) {
        return Mono.fromCallable(() -> toolCache.get(toolCode))
                .filter(tool -> tool != null && tool.isEnabled() && tool.isHealthy())
                .switchIfEmpty(Mono.empty())
                .doOnNext(tool -> log.debug("Found cached tool: {}", tool.getCode()));
    }

    public Optional<ApiTool> getCachedTool(String toolCode) {
        return Optional.ofNullable(toolCache.get(toolCode));
    }

    public void addOrUpdateTool(ApiTool tool) {
        if (tool.isEnabled() && tool.isHealthy()) {
            toolCache.put(tool.getCode(), tool);
            log.info("Tool {} added/updated in cache", tool.getCode());
        } else {
            toolCache.remove(tool.getCode());
            log.info("Tool {} removed from cache due to disabled state or unhealthy status", tool.getCode());
        }
    }

    public void removeTool(String toolCode) {
        toolCache.remove(toolCode);
        log.info("Tool {} removed from cache", toolCode);
    }

    /**
     * Versión reactiva para agregar/actualizar herramienta en el cache
     */
    public Mono<Void> addOrUpdateToolReactive(ApiTool tool) {
        return Mono.fromRunnable(() -> addOrUpdateTool(tool))
                .then();
    }

    /**
     * Recarga reactiva del cache desde la base de datos
     */
    public Mono<Integer> refreshCacheReactive() {
        return Mono.fromCallable(() -> apiToolRepository.findAllEnabled())
                .map(tools -> {
                    toolCache.clear();
                    tools.forEach(tool -> toolCache.put(tool.getCode(), tool));
                    log.info("Cache refreshed with {} tools", tools.size());
                    return tools.size();
                });
    }
}
