package org.dynamcorp.handsaiv2.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dynamcorp.handsaiv2.dto.AuthStatusResponse;
import org.dynamcorp.handsaiv2.model.AdminCredential;
import org.dynamcorp.handsaiv2.repository.AdminCredentialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    public static final String SESSION_COOKIE_NAME = "HANDSAI_SESSION";

    private final AdminCredentialRepository adminCredentialRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${handsai.auth.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${handsai.auth.lockout-duration-minutes:5}")
    private int lockoutDurationMinutes;

    @Value("${handsai.auth.session-timeout-minutes:15}")
    private int sessionTimeoutMinutes;

    /** sessionId → expiry instant */
    private final Map<String, Instant> activeSessions = new ConcurrentHashMap<>();

    // ─────────────────────── Setup ───────────────────────

    public boolean isSetupComplete() {
        return adminCredentialRepository.findFirst().isPresent();
    }

    @Transactional
    public AdminCredential registerAdmin(String username, String rawPassword) {
        if (isSetupComplete()) {
            throw new IllegalStateException("Admin already configured. Use the existing credentials.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        AdminCredential admin = AdminCredential.builder()
                .username(username.trim())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .failedLoginAttempts(0)
                .build();

        @SuppressWarnings("null")
        AdminCredential saved = adminCredentialRepository.save(admin);
        return saved;
    }

    // ─────────────────────── Login ───────────────────────

    @Transactional
    public String login(String username, String rawPassword) {
        AdminCredential admin = adminCredentialRepository.findFirst()
                .orElseThrow(() -> new IllegalStateException("Setup not completed."));

        // Check lockout
        if (isLocked(admin)) {
            throw new AccountLockedException(admin.getLockedUntil());
        }

        if (!admin.getUsername().equalsIgnoreCase(username.trim()) ||
                !passwordEncoder.matches(rawPassword, admin.getPasswordHash())) {
            // Increment failed attempts
            admin.setFailedLoginAttempts(admin.getFailedLoginAttempts() + 1);
            if (admin.getFailedLoginAttempts() >= maxFailedAttempts) {
                admin.setLockedUntil(Instant.now().plusSeconds(lockoutDurationMinutes * 60L));
            }
            adminCredentialRepository.save(admin);
            throw new InvalidCredentialsException(admin.getFailedLoginAttempts(), maxFailedAttempts);
        }

        // Successful login — reset counters
        admin.setFailedLoginAttempts(0);
        admin.setLockedUntil(null);
        adminCredentialRepository.save(admin);

        // Create session
        String sessionId = UUID.randomUUID().toString();
        activeSessions.put(sessionId, Instant.now().plusSeconds(sessionTimeoutMinutes * 60L));
        return sessionId;
    }

    public void logout(HttpServletRequest request) {
        String sessionId = extractSessionId(request);
        if (sessionId != null) {
            activeSessions.remove(sessionId);
        }
    }

    public boolean isSessionValid(HttpServletRequest request) {
        String sessionId = extractSessionId(request);
        if (sessionId == null)
            return false;
        Instant expiry = activeSessions.get(sessionId);
        if (expiry == null || Instant.now().isAfter(expiry)) {
            activeSessions.remove(sessionId);
            return false;
        }
        // Sliding window — extend on use
        activeSessions.put(sessionId, Instant.now().plusSeconds(sessionTimeoutMinutes * 60L));
        return true;
    }

    public AuthStatusResponse getStatus() {
        Optional<AdminCredential> adminOpt = adminCredentialRepository.findFirst();
        if (adminOpt.isEmpty()) {
            return new AuthStatusResponse(false, false, null, 0, maxFailedAttempts);
        }
        AdminCredential admin = adminOpt.get();
        boolean locked = isLocked(admin);
        return new AuthStatusResponse(
                true,
                locked,
                locked ? admin.getLockedUntil() : null,
                admin.getFailedLoginAttempts(),
                maxFailedAttempts);
    }

    public Cookie buildSessionCookie(String sessionId) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(sessionTimeoutMinutes * 60);
        return cookie;
    }

    public Cookie buildClearCookie() {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        return cookie;
    }

    // ─────────────────────── Helpers ───────────────────────

    private boolean isLocked(AdminCredential admin) {
        return admin.getLockedUntil() != null && Instant.now().isBefore(admin.getLockedUntil());
    }

    private String extractSessionId(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> SESSION_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    // ─────────────────────── Custom Exceptions ───────────────────────

    public static class AccountLockedException extends RuntimeException {
        private final Instant lockedUntil;

        public AccountLockedException(Instant lockedUntil) {
            super("Account locked until " + lockedUntil);
            this.lockedUntil = lockedUntil;
        }

        public Instant getLockedUntil() {
            return lockedUntil;
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        private final int attempts;
        private final int max;

        public InvalidCredentialsException(int attempts, int max) {
            super("Invalid credentials");
            this.attempts = attempts;
            this.max = max;
        }

        public int getAttempts() {
            return attempts;
        }

        public int getMax() {
            return max;
        }
    }
}
