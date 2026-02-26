package org.dynamcorp.handsaiv2.service;

import org.dynamcorp.handsaiv2.dto.ExportApiProviderDto;
import org.dynamcorp.handsaiv2.model.ApiProvider;
import org.dynamcorp.handsaiv2.model.ApiTool;
import org.dynamcorp.handsaiv2.model.AuthenticationTypeEnum;
import org.dynamcorp.handsaiv2.model.HttpMethodEnum;
import org.dynamcorp.handsaiv2.repository.ApiProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class ExportServiceTest {

        @Mock
        private ApiProviderRepository apiProviderRepository;

        @InjectMocks
        private ExportService exportService;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
        }

        @Test
        void exportProviders_masksApiKeyAndFiltersTools() {
                // Arrange
                ApiTool tool1 = ApiTool.builder()
                                .name("Tool 1")
                                .isExportable(true)
                                .build();
                ApiTool tool2 = ApiTool.builder()
                                .name("Tool 2")
                                .isExportable(false)
                                .build();

                ApiProvider providerWithKey = ApiProvider.builder()
                                .name("Provider 1")
                                .apiKeyValue("SUPER_SECRET_KEY")
                                .isExportable(true)
                                .tools(Arrays.asList(tool1, tool2))
                                .build();

                ApiProvider providerWithoutKey = ApiProvider.builder()
                                .name("Provider 2")
                                .apiKeyValue(null)
                                .isExportable(true)
                                .tools(List.of())
                                .build();

                when(apiProviderRepository.findByIsExportableTrue())
                                .thenReturn(Arrays.asList(providerWithKey, providerWithoutKey));

                // Act
                List<ExportApiProviderDto> result = exportService.exportProviders(null);

                // Assert
                assertEquals(2, result.size());

                ExportApiProviderDto exportedProvider1 = result.get(0);
                assertEquals("Provider 1", exportedProvider1.name());
                assertEquals("<YOUR_API_KEY>", exportedProvider1.apiKeyValue(), "API Key MUST be masked");
                assertEquals(2, exportedProvider1.tools().size(),
                                "All tools of exportable provider should be included");
                assertEquals("Tool 1", exportedProvider1.tools().get(0).name());
                assertEquals("Tool 2", exportedProvider1.tools().get(1).name());

                ExportApiProviderDto exportedProvider2 = result.get(1);
                assertEquals("Provider 2", exportedProvider2.name());
                assertNull(exportedProvider2.apiKeyValue(), "Null keys should remain null");
        }
}
