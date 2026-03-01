package org.dynamcorp.handsaiv2.dto;

public record RegenerateTokenResponse(
        String rawToken,
        String warning) {
}
