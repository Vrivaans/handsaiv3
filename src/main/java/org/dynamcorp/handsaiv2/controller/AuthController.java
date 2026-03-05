package org.dynamcorp.handsaiv2.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dynamcorp.handsaiv2.dto.AuthStatusResponse;
import org.dynamcorp.handsaiv2.dto.LoginRequest;
import org.dynamcorp.handsaiv2.dto.SetupRequest;
import org.dynamcorp.handsaiv2.dto.SetupResponse;
import org.dynamcorp.handsaiv2.service.AccessTokenService;
import org.dynamcorp.handsaiv2.service.AdminAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminAuthService adminAuthService;
    private final AccessTokenService accessTokenService;

    /** Public endpoint — shows setup state and lockout info */
    @GetMapping("/status")
    public AuthStatusResponse getStatus() {
        return adminAuthService.getStatus();
    }

    /** One-time setup: registers admin + generates first token */
    @PostMapping("/setup")
    public ResponseEntity<?> setup(@RequestBody SetupRequest request) {
        if (adminAuthService.isSetupComplete()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Setup already completed.",
                            "message", "Use your existing admin credentials."));
        }
        try {
            adminAuthService.registerAdmin(request.username(), request.password());
            String rawToken = accessTokenService.generateToken();

            return ResponseEntity.status(HttpStatus.CREATED).body(new SetupResponse(
                    rawToken,
                    "Save this token securely — it will never be shown again. " +
                            "Configure it in your MCP bridge as X-HandsAI-Token."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Login — returns session cookie */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            String sessionId = adminAuthService.login(request.username(), request.password());
            response.addCookie(adminAuthService.buildSessionCookie(sessionId));
            return ResponseEntity.ok(Map.of("success", true, "message", "Login successful."));
        } catch (AdminAuthService.AccountLockedException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "Account locked",
                    "message", "Too many failed attempts. Try again later.",
                    "retryAfter", e.getLockedUntil().toString()));
        } catch (AdminAuthService.InvalidCredentialsException e) {
            int remaining = e.getMax() - e.getAttempts();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Invalid credentials",
                    "message", "Wrong username or password.",
                    "attemptsRemaining", Math.max(0, remaining)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Logout — clears session cookie */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        adminAuthService.logout(request);
        response.addCookie(adminAuthService.buildClearCookie());
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** Check if current session is valid (for frontend to refresh state) */
    @GetMapping("/session")
    public ResponseEntity<?> checkSession(HttpServletRequest request) {
        boolean valid = adminAuthService.isSessionValid(request);
        return ResponseEntity.ok(Map.of("valid", valid));
    }
}
