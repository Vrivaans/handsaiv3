package org.dynamcorp.handsaiv2.dto;

import java.time.Instant;

public record AuthStatusResponse(
        boolean setupComplete,
        boolean locked,
        Instant lockedUntil,
        int failedAttempts,
        int maxAttempts) {
}
