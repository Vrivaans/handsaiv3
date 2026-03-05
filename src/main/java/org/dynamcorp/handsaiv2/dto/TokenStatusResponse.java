package org.dynamcorp.handsaiv2.dto;

import java.time.Instant;

public record TokenStatusResponse(
        boolean exists,
        String maskedToken,
        Instant createdAt,
        Instant lastUsedAt) {
}
