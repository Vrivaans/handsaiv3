package org.dynamcorp.handsaiv2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dynamcorp.handsaiv2.dto.ToolExecuteRequest;
import org.dynamcorp.handsaiv2.dto.ToolExecuteResponse;
import org.dynamcorp.handsaiv2.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ToolExecutionServiceTest {

    private MockRestServiceServer mockServer;

    @Mock
    private ApiToolService apiToolService;
    @Mock
    private ToolCacheManager toolCacheManager;
    @Mock
    private LogBatchProcessor logBatchProcessor;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private org.dynamcorp.handsaiv2.util.LogObfuscator logObfuscator;
    @Mock
    private DynamicTokenManager dynamicTokenManager;

    private ToolExecutionService service;
    private ApiTool tool;
    private ApiProvider provider;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        RestClient.Builder mockBuilder = mock(RestClient.Builder.class);
        RestClient restClient = RestClient.create(restTemplate);
        when(mockBuilder.baseUrl(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(restClient);

        service = new ToolExecutionService(
                apiToolService,
                toolCacheManager,
                logBatchProcessor,
                mockBuilder,
                objectMapper,
                encryptionService,
                logObfuscator,
                dynamicTokenManager);

        provider = new ApiProvider();
        provider.setId(10L);
        provider.setBaseUrl("https://api.test.com");
        provider.setAuthenticationType(AuthenticationTypeEnum.API_KEY);
        provider.setDynamicAuth(true);
        provider.setApiKeyLocation(ApiKeyLocationEnum.HEADER);
        provider.setApiKeyName("Authorization");

        tool = new ApiTool();
        tool.setId(100L);
        tool.setCode("TEST-TOOL");
        tool.setEnabled(true);
        tool.setHealthy(true);
        tool.setProvider(provider);
        tool.setEndpointPath("/data");
        tool.setHttpMethod(HttpMethodEnum.GET);
    }

    @Test
    void testExecuteApiTool_WithDynamicAuth_401TriggerRetrySuccess() {
        when(toolCacheManager.getCachedTool("TEST-TOOL")).thenReturn(Optional.of(tool));
        when(dynamicTokenManager.getToken(provider)).thenReturn("first-stale-token", "second-fresh-token");

        // 1st request returns 401 Unauthorized
        mockServer.expect(MockRestRequestMatchers.requestTo("/data"))
                .andExpect(MockRestRequestMatchers.header("Authorization", "first-stale-token"))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.UNAUTHORIZED));

        // 2nd request (retry) returns 200 OK
        mockServer.expect(MockRestRequestMatchers.requestTo("/data"))
                .andExpect(MockRestRequestMatchers.header("Authorization", "second-fresh-token"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"result\":\"ok\"}", MediaType.APPLICATION_JSON));

        ToolExecuteRequest request = new ToolExecuteRequest("TEST-TOOL", new HashMap<>(), "my-session-id");
        ToolExecuteResponse response = service.executeApiTool(request);

        assertNotNull(response);
        // Ensure that the mock errors don't surface as a failed execution silently
        if (response.errorMessage() != null) {
            System.err.println("Unexpected error message: " + response.errorMessage());
        }
        assertTrue(response.success());
        verify(dynamicTokenManager, times(1)).invalidateToken(10L); // it invalidated the stale token
        mockServer.verify();
    }

    @Test
    void testExecuteApiTool_WithDynamicAuth_401TriggerRetryFailsAgain() {
        when(toolCacheManager.getCachedTool("TEST-TOOL")).thenReturn(Optional.of(tool));
        when(dynamicTokenManager.getToken(provider)).thenReturn("first-token", "second-token");

        // 1st request 401
        mockServer.expect(MockRestRequestMatchers.requestTo("/data"))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.UNAUTHORIZED));

        // 2nd request 401 AGAIN
        mockServer.expect(MockRestRequestMatchers.requestTo("/data"))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.UNAUTHORIZED));

        ToolExecuteRequest request = new ToolExecuteRequest("TEST-TOOL", new HashMap<>(), "my-session-id");
        ToolExecuteResponse response = service.executeApiTool(request);

        assertFalse(response.success());
        // Since it's a 401 after refresh, it bubbles up as the same UNAUTHORIZED
        // exception, which is caught and its message stored.
        assertNotNull(response.errorMessage());
        assertTrue(response.errorMessage().contains("401"));

        verify(dynamicTokenManager, times(1)).invalidateToken(10L); // invalidated only once
        mockServer.verify();
    }
}
