package com.portfolio.VistaTrack.Services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
@Service
public class RefreshTokenService {

     private final FirestoreService firestoreService;
    private final JwtService       jwtService;

    private static final String COLLECTION        = "refresh_tokens";
    private static final long   EXPIRATION_DAYS   = 7;




     // ── SAVE ──────────────────────────────────────────────────────────────────

    /**
     * Persists a new refresh token in Firestore after a successful login.
     *
     * @param userId       the authenticated user's ID
     * @param refreshToken the opaque refresh token string (UUID)
     */
    public void saveRefreshToken(String userId, String refreshToken) {
        log.info("[REFRESH TOKEN] Saving refresh token for userId: {}", userId);

        Map<String, Object> doc = new HashMap<>();
        doc.put("userId",    userId);
        doc.put("token",     refreshToken);
        doc.put("createdAt", Instant.now().toString());
        doc.put("expiresAt", Instant.now().plus(EXPIRATION_DAYS, ChronoUnit.DAYS).toString());
        doc.put("revoked",   false);

        // Use the token itself as the document ID for O(1) direct lookup
        firestoreService.save(COLLECTION, refreshToken, doc);
        log.info("[REFRESH TOKEN] Refresh token saved for userId: {}", userId);
    }



     // ── VALIDATE ──────────────────────────────────────────────────────────────

    /**
     * Validates a refresh token — checks existence, revocation, and expiration.
     *
     * @param refreshToken the token to validate
     * @return the Firestore document if valid
     * @throws RuntimeException if the token is invalid, revoked, or expired
     */
    public Map<String, Object> validateRefreshToken(String refreshToken) {
        log.info("[REFRESH TOKEN] Validating refresh token");

        // Direct document lookup by token — no query needed
        Map<String, Object> doc = firestoreService.findById(COLLECTION, refreshToken)
                .orElseThrow(() -> {
                    log.warn("[REFRESH TOKEN] Token not found in Firestore");
                    return new RuntimeException("Invalid refresh token");
                });

        // Check revocation
        Boolean revoked = (Boolean) doc.get("revoked");
        if (Boolean.TRUE.equals(revoked)) {
            log.warn("[REFRESH TOKEN] Token has been revoked — possible reuse attack");
            // Revoke ALL tokens for this user — security measure against token theft
            revokeAllUserTokens((String) doc.get("userId"));
            throw new RuntimeException("Refresh token has been revoked");
        }

        // Check expiration
        String expiresAtStr = (String) doc.get("expiresAt");
        if (Instant.parse(expiresAtStr).isBefore(Instant.now())) {
            log.warn("[REFRESH TOKEN] Token has expired");
            throw new RuntimeException("Refresh token has expired — please log in again");
        }

        log.info("[REFRESH TOKEN] Token is valid for userId: {}", doc.get("userId"));
        return doc;
    }



    // ── ROTATE ────────────────────────────────────────────────────────────────

    /**
     * Rotates the refresh token on each use — revokes the old one and issues a new one.
     * This prevents token reuse and limits the damage of a stolen token.
     *
     * @param oldToken the refresh token that was just used
     * @param userId   the user's ID
     * @return the new refresh token string
     */
    public String rotateRefreshToken(String oldToken, String userId) {
        log.info("[REFRESH TOKEN] Rotating token for userId: {}", userId);

        // Revoke the old token
        Map<String, Object> update = new HashMap<>();
        update.put("revoked", true);
        firestoreService.save(COLLECTION, oldToken, update);
        log.debug("[REFRESH TOKEN] Old token revoked");

        // Issue a new refresh token
        String newToken = jwtService.generateRefreshToken(userId);
        saveRefreshToken(userId, newToken);
        log.info("[REFRESH TOKEN] New token issued for userId: {}", userId);

        return newToken;
    }


    // ── REVOKE ALL ────────────────────────────────────────────────────────────

    /**
     * Revokes all refresh tokens belonging to a user.
     * Called on logout or when a reuse attack is detected.
     *
     * @param userId the user whose tokens should all be revoked
     */
    public void revokeAllUserTokens(String userId) {
        log.info("[REFRESH TOKEN] Revoking all tokens for userId: {}", userId);

        List<Map<String, Object>> userTokens = firestoreService.findByField(
                COLLECTION, "userId", userId
        );

        userTokens.forEach(doc -> {
            String token = (String) doc.get("token");
            Map<String, Object> update = new HashMap<>();
            update.put("revoked", true);
            firestoreService.save(COLLECTION, token, update);
        });

        log.info("[REFRESH TOKEN] Revoked {} token(s) for userId: {}", userTokens.size(), userId);
    }


}
