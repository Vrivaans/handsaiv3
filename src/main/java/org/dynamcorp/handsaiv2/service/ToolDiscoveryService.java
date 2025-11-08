package org.dynamcorp.handsaiv2.service;

import java.time.Instant;

import org.dynamcorp.handsaiv2.dto.ToolDefinition;
import org.dynamcorp.handsaiv2.dto.ToolDiscoveryResponse;
import org.dynamcorp.handsaiv2.repository.ApiToolRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@RequiredArgsConstructor
public class ToolDiscoveryService {

    private final ToolCacheManager toolCacheManager;
    private final ApiToolRepository apiToolRepository;

    public Mono<ToolDiscoveryResponse> discoverTools() {
        log.info("Discovering available tools");

        return toolCacheManager.getAllCachedToolsReactive()
                .collectList()
                .flatMap(cachedTools -> {
                    if (cachedTools.isEmpty()) {
                        log.info("No tools available in cache, fetching from database");
                        return Mono.fromCallable(apiToolRepository::findAllEnabled)
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnNext(dbTools -> log.info("Discovered {} tools from database", dbTools.size()));
                    } else {
                        return Mono.just(cachedTools);
                    }
                })
                .flatMapMany(Flux::fromIterable)
                .map(ToolDefinition::from)
                .collectList()
                .map(toolDefinitions -> {
                    log.info("Discovered {} tools", toolDefinitions.size());
                    return new ToolDiscoveryResponse(
                            toolDefinitions,
                            toolDefinitions.size(),
                            Instant.now()
                    );
                })
                .doOnError(error -> log.error("Error during tool discovery", error))
                .onErrorReturn(new ToolDiscoveryResponse(
                        java.util.List.of(),
                        0,
                        Instant.now()
                ));
    }
}
