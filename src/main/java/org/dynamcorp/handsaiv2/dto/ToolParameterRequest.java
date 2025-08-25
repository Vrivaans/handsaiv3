package org.dynamcorp.handsaiv2.dto;

import org.dynamcorp.handsaiv2.model.ParameterType;

public record ToolParameterRequest(
    String name,
    ParameterType type,
    String description,
    Boolean required,
    String defaultValue
) {}
