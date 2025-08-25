package org.dynamcorp.handsaiv2.dto;

import org.dynamcorp.handsaiv2.model.ApiTool;

import java.util.Map;

public record ToolDefinition(
    String name,
    String description,
    String type,
    Map<String, Object> parameters
) {
    public static ToolDefinition from(ApiTool apiTool) {
        // Crear la estructura de parámetros compatible con OpenAI function calling
        Map<String, Object> parametersSchema = Map.of(
            "type", "object",
            "properties", apiTool.getParameters().stream()
                .collect(java.util.stream.Collectors.toMap(
                    param -> param.getName(),
                    param -> Map.of(
                        "type", param.getType().toString().toLowerCase(),
                        "description", param.getDescription()
                    )
                )),
            "required", apiTool.getParameters().stream()
                .filter(param -> param.getRequired())
                .map(param -> param.getName())
                .collect(java.util.stream.Collectors.toList())
        );

        return new ToolDefinition(
            apiTool.getCode(),
            apiTool.getDescription(),
            "api_tool",
            parametersSchema
        );
    }
}
