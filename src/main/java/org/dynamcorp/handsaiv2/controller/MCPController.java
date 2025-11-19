package org.dynamcorp.handsaiv2.controller;

import java.util.List;

import org.dynamcorp.handsaiv2.dto.McpContent;
import org.dynamcorp.handsaiv2.dto.McpError;
import org.dynamcorp.handsaiv2.dto.McpResponse;
import org.dynamcorp.handsaiv2.dto.McpTool;
import org.dynamcorp.handsaiv2.dto.McpToolCallRequest;
import org.dynamcorp.handsaiv2.dto.McpToolCallResponse;
import org.dynamcorp.handsaiv2.dto.McpToolsListResponse;
import org.dynamcorp.handsaiv2.dto.ToolDefinition;
import org.dynamcorp.handsaiv2.dto.ToolDiscoveryResponse;
import org.dynamcorp.handsaiv2.dto.ToolExecuteRequest;
import org.dynamcorp.handsaiv2.dto.ToolExecuteResponse;
import org.dynamcorp.handsaiv2.service.ToolDiscoveryService;
import org.dynamcorp.handsaiv2.service.ToolExecutionService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MCPController {

        private final ToolDiscoveryService toolDiscoveryService;
        private final ToolExecutionService toolExecutionService;

        @GetMapping("/tools/list")
        public McpResponse<McpToolsListResponse> discoverTools() {
                try {
                        ToolDiscoveryResponse result = toolDiscoveryService.discoverTools();
                        McpToolsListResponse mcpResult = convertToMcpToolsList(result);

                        return McpResponse.<McpToolsListResponse>builder()
                                        .jsonrpc("2.0")
                                        .result(mcpResult)
                                        .build();
                } catch (Exception ex) {
                        return McpResponse.<McpToolsListResponse>builder()
                                        .jsonrpc("2.0")
                                        .error(McpError.builder()
                                                        .code(-32603)
                                                        .message("Internal error discovering tools")
                                                        .build())
                                        .build();
                }
        }

        @PostMapping("/tools/call")
        public McpResponse<McpToolCallResponse> executeApiTool(@RequestBody McpToolCallRequest request) {
                // Validar request
                if (request == null || request.params() == null) {
                        return McpResponse.<McpToolCallResponse>builder()
                                        .jsonrpc("2.0")
                                        .error(McpError.builder()
                                                        .code(-32602)
                                                        .message("Invalid params: missing required parameters")
                                                        .build())
                                        .id(request != null ? request.id() : null)
                                        .build();
                }

                try {
                        // Convertir request MCP a ToolExecuteRequest
                        ToolExecuteRequest toolRequest = new ToolExecuteRequest(
                                        request.params().name(),
                                        request.params().arguments(),
                                        null // sessionId no es requerido en MCP
                        );

                        ToolExecuteResponse response = toolExecutionService.executeApiTool(toolRequest);
                        McpToolCallResponse mcpResult = convertToMcpToolCall(response);

                        return McpResponse.<McpToolCallResponse>builder()
                                        .jsonrpc("2.0")
                                        .result(mcpResult)
                                        .id(request.id())
                                        .build();

                } catch (Exception ex) {
                        return McpResponse.<McpToolCallResponse>builder()
                                        .jsonrpc("2.0")
                                        .error(McpError.builder()
                                                        .code(getErrorCode(ex))
                                                        .message(getErrorMessage(ex))
                                                        .build())
                                        .id(request.id())
                                        .build();
                }
        }

        private McpToolsListResponse convertToMcpToolsList(ToolDiscoveryResponse response) {
                List<McpTool> mcpTools = response.tools().stream()
                                .map(this::convertToMcpTool)
                                .toList();

                return McpToolsListResponse.builder()
                                .tools(mcpTools)
                                .build();
        }

        private McpTool convertToMcpTool(ToolDefinition toolDef) {
                return McpTool.builder()
                                .name(toolDef.name())
                                .description(toolDef.description())
                                .inputSchema(toolDef.parameters())
                                .build();
        }

        private McpToolCallResponse convertToMcpToolCall(ToolExecuteResponse response) {
                String textContent = response.success()
                                ? (response.result() != null ? response.result().toString() : "")
                                : (response.errorMessage() != null ? response.errorMessage()
                                                : "Error ejecutando herramienta");

                McpContent content = McpContent.builder()
                                .type("text")
                                .text(textContent)
                                .build();

                return McpToolCallResponse.builder()
                                .content(List.of(content))
                                .build();
        }

        private int getErrorCode(Throwable ex) {
                if (ex instanceof IllegalArgumentException) {
                        return -32602; // Invalid params
                }
                return -32603; // Internal error
        }

        private String getErrorMessage(Throwable ex) {
                if (ex instanceof IllegalArgumentException) {
                        return "Invalid params: " + ex.getMessage();
                }
                return "Internal error: " + ex.getMessage();
        }
}
