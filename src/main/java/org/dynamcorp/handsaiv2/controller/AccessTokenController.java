package org.dynamcorp.handsaiv2.controller;

import lombok.RequiredArgsConstructor;
import org.dynamcorp.handsaiv2.dto.RegenerateTokenResponse;
import org.dynamcorp.handsaiv2.dto.TokenStatusResponse;
import org.dynamcorp.handsaiv2.service.AccessTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/token")
@RequiredArgsConstructor
public class AccessTokenController {

    private final AccessTokenService accessTokenService;

    /** Returns token status (masked) — requires admin session */
    @GetMapping("/status")
    public TokenStatusResponse getStatus() {
        return accessTokenService.getStatus();
    }

    /** Regenerates the token — requires admin session. Returns raw token once. */
    @PostMapping("/regenerate")
    public ResponseEntity<RegenerateTokenResponse> regenerate() {
        String rawToken = accessTokenService.generateToken();
        return ResponseEntity.ok(new RegenerateTokenResponse(
                rawToken,
                "Save this token securely — it will never be shown again. " +
                        "If your MCP bridge was using the previous token, update it now."));
    }
}
