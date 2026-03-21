package com.portfolio.VistaTrack.Services;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.portfolio.VistaTrack.Dto.LoginRequestDto;
import com.portfolio.VistaTrack.Dto.ResponseDto;
import com.portfolio.VistaTrack.Dto.SignupRequestDto;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class AuthService {

    private final PasswordEncoder  passwordEncoder;
    private final JwtService       jwtService;
    private final FirestoreService firestoreService;
    private final RefreshTokenService refreshTokenService;

    // Firestore collection name — centralised to avoid typos across the codebase
    private static final String USERS_COLLECTION = "users";

    // ── REGISTER ───────────────────────────────────────────────────────────────

    /**
     * Creates a new user account and persists it in Firestore.
     *
     * Steps:
     * 
     *   Check that no existing user has the same email
     *   Generate a unique userId (UUID)
     *   Hash the password with BCrypt
     *   Build and save the user document in Firestore
     *   Return HTTP 201 with a confirmation message
     * 
     * 
     *
     * @param request DTO containing the new user's email, username, and raw password
     * @return ResponseEntity with HTTP 201 and a success message
     * @throws RuntimeException if a user with this email already exists
     */
    public ResponseEntity<ResponseDto> register(SignupRequestDto request) {
        log.info("[AUTH] Registration attempt for email: {}", request.getEmail());

        // ── 1. Check email uniqueness ──────────────────────────────────────────
        // Query Firestore for any existing document with this email
        List<Map<String, Object>> existing = firestoreService.findByField(
                USERS_COLLECTION, "email", request.getEmail()
        );

        if (!existing.isEmpty()) {
            log.warn("[AUTH] Registration failed — email already in use: {}", request.getEmail());
            throw new RuntimeException("An account with this email already exists");
        }

        // ── 2. Generate a unique ID for this user ──────────────────────────────
        String userId = UUID.randomUUID().toString();

        // ── 3. Hash the password — never store plain text ──────────────────────
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // ── 4. Build the Firestore document ────────────────────────────────────
        Map<String, Object> userDocument = new HashMap<>();
        userDocument.put("id",        userId);
        userDocument.put("username",  request.getUsername());
        userDocument.put("email",     request.getEmail());
        userDocument.put("password",  hashedPassword);          // BCrypt hash only
        userDocument.put("role",      "USER");                  // default role
        userDocument.put("active",    true);
        userDocument.put("createdAt", Instant.now().toString()); // ISO-8601 timestamp

        // ── 5. Persist in Firestore ────────────────────────────────────────────
        firestoreService.save(USERS_COLLECTION, userId, userDocument);
        log.info("[AUTH] User registered successfully — userId: {} | email: {}",
                userId, request.getEmail());

        // ── 6. Return 201 Created ──────────────────────────────────────────────
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto(201, "Account created successfully for " + request.getEmail(), ""));
    }



       // ── LOGIN ──────────────────────────────────────────────────────────────────

    /**
     * Authenticates a user and returns a signed JWT token on success.
     *
     * <p>Steps:
     * <ol>
     *   <li>Find the user document in Firestore by email</li>
     *   <li>Verify the raw password against the stored BCrypt hash</li>
     *   <li>Generate and return a signed JWT</li>
     * </ol>
     * </p>
     *
     * @param request DTO containing the user's email and raw password
     * @return ResponseEntity with HTTP 200 and the JWT token in the body
     * @throws RuntimeException if the email is not found or the password is incorrect
     */
    
    

public ResponseEntity<ResponseDto> login(LoginRequestDto request) {
    log.info("[AUTH] Login attempt for email: {}", request.getEmail());

    List<Map<String, Object>> results = firestoreService.findByField(
            USERS_COLLECTION, "email", request.getEmail()
    );
    if (results.isEmpty()) {
        log.warn("[AUTH] Login failed — no account found for email: {}", request.getEmail());
        throw new RuntimeException("Invalid email or password");
    }

    Map<String, Object> userDocument = results.get(0);
    String storedHash = (String) userDocument.get("password");

    if (!passwordEncoder.matches(request.getPassword(), storedHash)) {
        log.warn("[AUTH] Login failed — wrong password for email: {}", request.getEmail());
        throw new RuntimeException("Invalid email or password");
    }

    String userId = (String) userDocument.get("id");
    String email  = (String) userDocument.get("email");
    String role   = (String) userDocument.get("role");

    // Generate both tokens
    String accessToken  = jwtService.generateToken(userId, email, role);
    String refreshToken = jwtService.generateRefreshToken(userId);

    // Save refresh token in Firestore
    refreshTokenService.saveRefreshToken(userId, refreshToken);

    log.info("[AUTH] Login successful — userId: {}", userId);

    // Return both tokens in the response
    Map<String, String> tokens = new HashMap<>();
    tokens.put("accessToken",  accessToken);
    tokens.put("refreshToken", refreshToken);

    return ResponseEntity.ok(new ResponseDto(200, "Login successful", tokens));
}

// ── ADD refresh() ──────────────────────────────────────────────────────────

/**
 * Issues a new access token using a valid refresh token.
 * Rotates the refresh token on each use (old one is revoked, new one is issued).
 *
 * @param refreshToken the client's current refresh token
 * @return ResponseEntity with a new access token and a new refresh token
 */
public ResponseEntity<ResponseDto> refresh(String refreshToken) {
    log.info("[AUTH] Token refresh request received");

    // Validate and get the token document from Firestore
    Map<String, Object> tokenDoc = refreshTokenService.validateRefreshToken(refreshToken);
    String userId = (String) tokenDoc.get("userId");

    // Load the user to get current email and role
    Map<String, Object> userDoc = firestoreService.findById(USERS_COLLECTION, userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    String email = (String) userDoc.get("email");
    String role  = (String) userDoc.get("role");

    // Rotate the refresh token and issue a new access token
    String newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken, userId);
    String newAccessToken  = jwtService.generateToken(userId, email, role);

    log.info("[AUTH] Tokens refreshed successfully for userId: {}", userId);

    Map<String, String> tokens = new HashMap<>();
    tokens.put("accessToken",  newAccessToken);
    tokens.put("refreshToken", newRefreshToken);

    return ResponseEntity.ok(new ResponseDto(200, "Tokens refreshed successfully", tokens));
}

// ── ADD logout() ───────────────────────────────────────────────────────────

/**
 * Logs out the user by revoking all their refresh tokens.
 * The access token will naturally expire after 15 minutes.
 *
 * @param userId the authenticated user's ID from the JWT
 * @return ResponseEntity with a confirmation message
 */
public ResponseEntity<ResponseDto> logout(String userId) {
    log.info("[AUTH] Logout request for userId: {}", userId);
    refreshTokenService.revokeAllUserTokens(userId);
    log.info("[AUTH] User logged out — all tokens revoked for userId: {}", userId);
    return ResponseEntity.ok(new ResponseDto(200, "Logged out successfully", null));
}
}

