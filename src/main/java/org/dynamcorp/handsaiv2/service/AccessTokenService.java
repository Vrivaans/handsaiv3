package org.dynamcorp.handsaiv2.service;

import lombok.RequiredArgsConstructor;
import org.dynamcorp.handsaiv2.dto.TokenStatusResponse;
import org.dynamcorp.handsaiv2.model.AccessToken;
import org.dynamcorp.handsaiv2.repository.AccessTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessTokenService {

    private final AccessTokenRepository accessTokenRepository;
    private final EncryptionService encryptionService;

    /**
     * Generates a new PAT token, replaces any existing one.
     * Returns the RAW token — caller must store/show it; it will never be
     * retrievable again.
     */
    @Transactional
    public String generateToken() {
        // Delete any existing token
        accessTokenRepository.deleteAll();

        String rawToken = UUID.randomUUID().toString();
        String encrypted = encryptionService.encrypt(rawToken);

        AccessToken token = AccessToken.builder()
                .encryptedToken(encrypted)
                .build();

        @SuppressWarnings("null")
        AccessToken saved = accessTokenRepository.save(token);
        // discard saved as it's not needed for the return
        return rawToken;
    }

    /**
     * Validates an incoming raw token against the stored encrypted one.
     * Updates lastUsedAt on success.
     */
    @Transactional
    public boolean validateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank())
            return false;

        Optional<AccessToken> tokenOpt = accessTokenRepository.findFirst();
        if (tokenOpt.isEmpty())
            return false;

        AccessToken stored = tokenOpt.get();
        try {
            String decrypted = encryptionService.decrypt(stored.getEncryptedToken());
            if (!decrypted.equals(rawToken))
                return false;
            stored.setLastUsedAt(Instant.now());
            accessTokenRepository.save(stored);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public TokenStatusResponse getStatus() {
        Optional<AccessToken> tokenOpt = accessTokenRepository.findFirst();
        if (tokenOpt.isEmpty()) {
            return new TokenStatusResponse(false, null, null, null);
        }
        AccessToken token = tokenOpt.get();
        String masked = buildMasked(token.getEncryptedToken());
        return new TokenStatusResponse(true, masked, token.getCreatedAt(), token.getLastUsedAt());
    }

    public boolean hasToken() {
        return accessTokenRepository.findFirst().isPresent();
    }

    private String buildMasked(String encryptedToken) {
        // Show first 8 chars of the encrypted value as a fingerprint (not sensitive)
        if (encryptedToken == null || encryptedToken.length() < 8)
            return "****";
        return encryptedToken.substring(0, 8) + "...";
    }
}
