package org.dynamcorp.handsaiv2.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.dynamcorp.handsaiv2.dto.ToolDefinition;
import org.dynamcorp.handsaiv2.dto.ToolDiscoveryResponse;
import org.dynamcorp.handsaiv2.model.ApiTool;
import org.dynamcorp.handsaiv2.repository.ApiToolRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ToolDiscoveryService {

    private final ToolCacheManager toolCacheManager;
    private final ApiToolRepository apiToolRepository;

    public ToolDiscoveryResponse discoverTools() {
        log.info("Discovering available tools");

        try {
            List<ApiTool> tools = toolCacheManager.getAllCachedTools();

            if (tools.isEmpty()) {
                log.info("No tools available in cache, fetching from database");
                tools = apiToolRepository.findAllEnabled();
                log.info("Discovered {} tools from database", tools.size());
            }

            List<ToolDefinition> toolDefinitions = tools.stream()
                    .map(ToolDefinition::from)
                    .collect(Collectors.toList());

            log.info("Discovered {} tools", toolDefinitions.size());
            return new ToolDiscoveryResponse(
                    toolDefinitions,
                    toolDefinitions.size(),
                    Instant.now());
        } catch (Exception error) {
            log.error("Error during tool discovery", error);
            return new ToolDiscoveryResponse(
                    List.of(),
                    0,
                    Instant.now());
        }
    }
}
