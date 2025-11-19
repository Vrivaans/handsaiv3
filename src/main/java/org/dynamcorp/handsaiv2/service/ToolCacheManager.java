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
        return toolCache.values().stream()
                .filter(tool -> tool.isEnabled() && tool.isHealthy())
                .toList();
    }

    public Optional<ApiTool> getCachedTool(String toolCode) {
        return Optional.ofNullable(toolCache.get(toolCode))
                .filter(tool -> tool.isEnabled() && tool.isHealthy());
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

    public int refreshCache() {
        List<ApiTool> tools = apiToolRepository.findAllEnabled();
        toolCache.clear();
        tools.forEach(tool -> toolCache.put(tool.getCode(), tool));
        log.info("Cache refreshed with {} tools", tools.size());
        return tools.size();
    }
}
