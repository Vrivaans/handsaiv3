package org.dynamcorp.handsaiv2.dto;

public record ExportToolParameterDto(
        String name,
        String type,
        String description,
        Boolean required,
        String defaultValue) {
}
