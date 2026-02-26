package org.dynamcorp.handsaiv2.dto;

import org.dynamcorp.handsaiv2.model.ParameterType;

public record ImportToolParameterRequest(
                String name,
                ParameterType type,
                String description,
                Boolean required,
                String defaultValue) {
}
