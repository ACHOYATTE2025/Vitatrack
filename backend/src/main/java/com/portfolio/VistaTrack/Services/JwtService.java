package com.portfolio.VistaTrack.Services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Handles all JWT operations: token generation, validation, and claim extraction.
 * Tokens are signed with HMAC-SHA256 using a secret key defined in application.properties.
 *
 * Required properties:
 *   jwt.key        — secret key (min 256 bits, injected from JWT_KEY env variable)
 *   jwt.expiration — token lifetime in milliseconds (e.g. 86400000 = 24h)
 */
@Slf4j
@Service
public class JwtService {

    @Value("${jwt.key}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationMs;

    // ── TOKEN GENERATION ───────────────────────────────────────────────────────

    /**
     * Generates a signed JWT token embedding the user's identity and role.
     *
     * @param userId the user's unique ID — stored as the JWT subject
     * @param email  the user's email — stored as a custom claim
     * @param role   the user's role (e.g. "USER") — stored as a custom claim
     * @return a signed, compact JWT string ready to send to the client
     */
    public String generateToken(String userId, String email, String role) {
        log.info("[JWT] Generating token for userId: {}", userId);

        String token = Jwts.builder()
                .subject(userId)                                                  // who the token belongs to
                .claim("email", email)                                            // email claim
                .claim("role", role)                                              // role claim for authorization
                .issuedAt(new Date())                                             // issued at current time
                .expiration(new Date(System.currentTimeMillis() + expirationMs)) // expires after configured duration
                .signWith(getSigningKey())                                        // signed with HMAC-SHA256
                .compact();

        log.info("[JWT] Token generated successfully for userId: {}", userId);
        return token;
    }

    // ── TOKEN VALIDATION ───────────────────────────────────────────────────────

    /**
     * Validates a JWT token by checking its signature and expiration date.
     *
     * @param token the raw JWT string (without the "Bearer " prefix)
     * @return true if the token is valid and not expired, false otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            getClaims(token); // throws JwtException if signature is invalid or token is expired
            log.debug("[JWT] Token is valid");
            return true;
        } catch (Exception e) {
            log.warn("[JWT] Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ── CLAIM EXTRACTION ───────────────────────────────────────────────────────

    /**
     * Extracts the userId from the token subject.
     *
     * @param token the raw JWT string
     * @return the userId stored as the JWT subject
     */
    public String extractUserId(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extracts the email from the token claims.
     *
     * @param token the raw JWT string
     * @return the email stored in the token
     */
    public String extractEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    /**
     * Extracts the role from the token claims.
     *
     * @param token the raw JWT string
     * @return the role stored in the token (e.g. "USER")
     */
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // ── PRIVATE HELPERS ────────────────────────────────────────────────────────

    /**
     * Parses and returns all claims from the token.
     * Throws a JwtException if the token is tampered, expired, or has an invalid signature.
     *
     * @param token the raw JWT string
     * @return parsed Claims object containing all token data
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Builds the HMAC-SHA256 signing key from the configured secret string.
     * The secret must be at least 256 bits (32 characters) long.
     *
     * @return SecretKey used to sign and verify tokens
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
 * Generates a long-lived refresh token (7 days).
 * Stored in Firestore — used only to obtain a new access token.
 * Uses UUID so it is opaque and cannot be decoded to extract user info.
 *
 * @param userId the user's unique ID
 * @return a random UUID string used as the refresh token
 */
public String generateRefreshToken(String userId) {
    log.info("[JWT] Generating refresh token for userId: {}", userId);
    return UUID.randomUUID().toString(); // opaque token — not a JWT
}
}