package org.dynamcorp.handsaiv2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dynamcorp.handsaiv2.dto.ExportApiProviderDto;
import org.dynamcorp.handsaiv2.dto.ExportApiToolDto;
import org.dynamcorp.handsaiv2.dto.ExportToolParameterDto;
import org.dynamcorp.handsaiv2.model.ApiProvider;
import org.dynamcorp.handsaiv2.model.ApiTool;
import org.dynamcorp.handsaiv2.repository.ApiProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final ApiProviderRepository apiProviderRepository;
    private static final String MASKED_API_KEY = "<YOUR_API_KEY>";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<ExportApiProviderDto> exportProviders(List<Long> providerIds) {
        log.info("Exporting providers. Target IDs: {}", providerIds != null ? providerIds : "ALL");

        List<ApiProvider> providers;
        if (providerIds == null || providerIds.isEmpty()) {
            providers = apiProviderRepository.findByIsExportableTrue();
        } else {
            providers = apiProviderRepository.findByIdInAndIsExportableTrue(providerIds);
        }

        // Apply strict security masking
        return providers.stream()
                .map(this::mapToExportProviderDto)
                .collect(Collectors.toList());
    }

    private ExportApiProviderDto mapToExportProviderDto(ApiProvider provider) {
        List<ExportApiToolDto> exportTools = provider.getTools().stream()
                .map(this::mapToExportToolDto)
                .collect(Collectors.toList());

        Map<String, String> customHeaders = null;
        if (provider.getCustomHeadersJson() != null && !provider.getCustomHeadersJson().isEmpty()) {
            try {
                customHeaders = objectMapper.readValue(provider.getCustomHeadersJson(),
                        new TypeReference<Map<String, String>>() {
                        });
            } catch (Exception e) {
                log.error("Failed to parse customHeadersJson for export on provider {}", provider.getId(), e);
            }
        }

        return new ExportApiProviderDto(
                provider.getName(),
                provider.getCode(),
                provider.getBaseUrl(),
                provider.getAuthenticationType(),
                provider.getApiKeyLocation(),
                provider.getApiKeyName(),
                provider.getApiKeyValue() != null ? MASKED_API_KEY : null,
                customHeaders,
                exportTools);
    }

    private ExportApiToolDto mapToExportToolDto(ApiTool tool) {
        List<ExportToolParameterDto> exportParams = tool.getParameters().stream()
                .map(param -> new ExportToolParameterDto(
                        param.getName(),
                        param.getType() != null ? param.getType().name() : null,
                        param.getDescription(),
                        param.getRequired(),
                        param.getDefaultValue()))
                .collect(Collectors.toList());

        return new ExportApiToolDto(
                tool.getName(),
                tool.getCode(),
                tool.getDescription(),
                tool.getEndpointPath(),
                tool.getHttpMethod(),
                exportParams);
    }
}
