package org.dynamcorp.handsaiv2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dynamcorp.handsaiv2.dto.ImportApiProviderRequest;
import org.dynamcorp.handsaiv2.dto.ImportApiToolRequest;
import org.dynamcorp.handsaiv2.dto.ImportToolParameterRequest;
import org.dynamcorp.handsaiv2.model.ApiProvider;
import org.dynamcorp.handsaiv2.model.ApiTool;
import org.dynamcorp.handsaiv2.model.ToolParameter;
import org.dynamcorp.handsaiv2.repository.ApiProviderRepository;
import org.dynamcorp.handsaiv2.repository.ApiToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final ApiProviderRepository providerRepository;
    private final ApiToolRepository toolRepository;
    private final EncryptionService encryptionService;
    private final ToolCacheManager toolCacheManager;

    private static final String MASKED_API_KEY = "<YOUR_API_KEY>";

    @Transactional
    public void importProviders(List<ImportApiProviderRequest> importRequests) {
        log.info("Starting import of {} providers.", importRequests.size());

        for (ImportApiProviderRequest providerReq : importRequests) {
            ApiProvider provider = processProvider(providerReq);

            if (providerReq.tools() != null) {
                for (ImportApiToolRequest toolReq : providerReq.tools()) {
                    processTool(toolReq, provider);
                }
            }
        }

        toolCacheManager.refreshCache();
        log.info("Import completed successfully.");
    }

    private ApiProvider processProvider(ImportApiProviderRequest req) {
        String code = (req.code() != null && !req.code().isBlank()) ? req.code() : UUID.randomUUID().toString();
        ApiProvider provider = providerRepository.findByCode(code).orElse(null);

        if (provider == null) {
            log.info("Creating imported provider: {}", req.name());
            provider = new ApiProvider();
            provider.setCode(code);
            provider.setCreatedAt(Instant.now());
        } else {
            log.info("Updating existing provider: {}", req.name());
            // Existing ones usually don't need code reassignment, but safe to do
            provider.setCode(code);
        }

        provider.setName(req.name());
        provider.setBaseUrl(req.baseUrl());
        provider.setAuthenticationType(req.authenticationType());
        provider.setApiKeyLocation(req.apiKeyLocation());
        provider.setApiKeyName(req.apiKeyName());
        provider.setExportable(true); // Since it was exported/imported

        // Only update API Key if it's NOT the masked placeholder and NOT empty
        if (req.apiKeyValue() != null && !req.apiKeyValue().isBlank() && !req.apiKeyValue().equals(MASKED_API_KEY)) {
            provider.setApiKeyValue(encryptionService.encrypt(req.apiKeyValue()));
        }

        provider.setUpdatedAt(Instant.now());
        return providerRepository.save(provider);
    }

    private void processTool(ImportApiToolRequest req, ApiProvider provider) {
        String code = (req.code() != null && !req.code().isBlank()) ? req.code() : UUID.randomUUID().toString();
        ApiTool tool = toolRepository.findByCode(code).orElse(null);

        if (tool == null) {
            log.info("Creating imported tool: {}", req.name());
            tool = new ApiTool();
            tool.setCode(code);
            tool.setCreatedAt(Instant.now());
            tool.setParameters(new LinkedHashSet<>());
        } else {
            log.info("Updating existing tool: {}", req.name());
            tool.getParameters().clear(); // To replace with fresh parameter list
        }

        tool.setProvider(provider);
        tool.setName(req.name());
        tool.setDescription(req.description());
        tool.setEndpointPath(req.endpointPath());
        tool.setHttpMethod(req.httpMethod());
        tool.setEnabled(true);
        tool.setHealthy(true);
        tool.setExportable(true);
        tool.setUpdatedAt(Instant.now());

        final ApiTool finalTool = tool;

        if (req.parameters() != null) {
            Set<ToolParameter> parameters = req.parameters().stream()
                    .map(pReq -> processParameter(pReq, finalTool))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            tool.getParameters().addAll(parameters);
        }

        toolRepository.save(tool);
    }

    private ToolParameter processParameter(ImportToolParameterRequest req, ApiTool tool) {
        return ToolParameter.builder()
                .apiTool(tool)
                .name(req.name())
                .code(UUID.randomUUID().toString()) // Re-roll parameter UUIDs on import to avoid db conflicts
                .type(req.type())
                .description(req.description())
                .required(req.required())
                .defaultValue(req.defaultValue())
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
