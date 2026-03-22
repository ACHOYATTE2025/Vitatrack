package com.portfolio.VistaTrack;



import com.portfolio.VistaTrack.Dto.LoginRequestDto;
import com.portfolio.VistaTrack.Dto.ResponseDto;
import com.portfolio.VistaTrack.Dto.SignupRequestDto;
import com.portfolio.VistaTrack.Services.AuthService;
import com.portfolio.VistaTrack.Services.FirestoreService;
import com.portfolio.VistaTrack.Services.JwtService;
import com.portfolio.VistaTrack.Services.RefreshTokenService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 *
 * All dependencies are mocked — no Firestore, no JWT crypto, no BCrypt calls.
 * Each test verifies one specific behavior in isolation.
 *
 * Coverage:
 *   register() — success, duplicate email
 *   login()    — success, email not found, wrong password
 *   refresh()  — success, invalid token, user not found
 *   logout()   — success
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    // ── Mocks ──────────────────────────────────────────────────────────────────
    @Mock private PasswordEncoder     passwordEncoder;
    @Mock private JwtService          jwtService;
    @Mock private FirestoreService    firestoreService;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private AuthService authService;

    // ── Test constants ─────────────────────────────────────────────────────────
    private static final String USER_ID       = "user-123";
    private static final String EMAIL         = "john@example.com";
    private static final String USERNAME      = "johndoe";
    private static final String RAW_PASSWORD  = "password123";
    private static final String HASHED_PW     = "$2a$10$hashedpassword";
    private static final String ACCESS_TOKEN  = "eyJhbGciOiJIUzM4NCJ9.access";
    private static final String REFRESH_TOKEN = "550e8400-e29b-41d4-a716-446655440000";
    private static final String NEW_ACCESS    = "eyJhbGciOiJIUzM4NCJ9.newaccess";
    private static final String NEW_REFRESH   = "new-refresh-uuid-9999";

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Builds a minimal user document as stored in Firestore */
    private Map<String, Object> buildUserDoc() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id",       USER_ID);
        doc.put("email",    EMAIL);
        doc.put("username", USERNAME);
        doc.put("password", HASHED_PW);
        doc.put("role",     "USER");
        doc.put("active",   true);
        return doc;
    }

    /** Builds a SignupRequestDto */
    private SignupRequestDto signupRequest() {
        return new SignupRequestDto(USERNAME, EMAIL, RAW_PASSWORD);
    }

    /** Builds a LoginRequestDto */
    private LoginRequestDto loginRequest() {
        return new LoginRequestDto(EMAIL, RAW_PASSWORD);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // register()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("✅ returns HTTP 201 when account created successfully")
        void returns201WhenSuccess() {
            // Arrange — no existing user with this email
            when(firestoreService.findByField("users", "email", EMAIL))
                .thenReturn(List.of());
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PW);

            // Act
            ResponseEntity<ResponseDto> response = authService.register(signupRequest());

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatusCode()).isEqualTo(201);
            assertThat(response.getBody().getStatusMsg()).contains(EMAIL);

            // Verify user was saved to Firestore
            verify(firestoreService).save(eq("users"), anyString(), anyMap());
        }

        @Test
        @DisplayName("✅ saves hashed password — never stores raw password")
        void savesHashedPassword() {
            // Arrange
            when(firestoreService.findByField("users", "email", EMAIL))
                .thenReturn(List.of());
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PW);

            // Act
            authService.register(signupRequest());

            // Capture saved document
            @SuppressWarnings("unchecked")
            var captor = org.mockito.ArgumentCaptor
                .forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
            verify(firestoreService).save(eq("users"), anyString(), captor.capture());

            Map<String, Object> saved = captor.getValue();
            assertThat(saved.get("password")).isEqualTo(HASHED_PW);
            assertThat(saved.get("password")).isNotEqualTo(RAW_PASSWORD);
            assertThat(saved.get("email")).isEqualTo(EMAIL);
            assertThat(saved.get("username")).isEqualTo(USERNAME);
            assertThat(saved.get("role")).isEqualTo("USER");
            assertThat(saved.get("active")).isEqualTo(true);
            assertThat(saved.get("id")).isNotNull();
            assertThat(saved.get("createdAt")).isNotNull();
        }

        @Test
        @DisplayName("❌ throws RuntimeException when email already exists")
        void throwsWhenEmailAlreadyExists() {
            // Arrange — email already taken
            when(firestoreService.findByField("users", "email", EMAIL))
                .thenReturn(List.of(buildUserDoc()));

            // Act & Assert
            assertThatThrownBy(() -> authService.register(signupRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");

            // Verify nothing was saved
            verify(firestoreService, never()).save(any(), any(), any());
            verify(passwordEncoder, never()).encode(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // login()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("✅ returns HTTP 200 with accessToken and refreshToken on success")
        void returns200WithTokensOnSuccess() {
            // Arrange
            when(firestoreService.findByField("users", "email", EMAIL))
                .thenReturn(List.of(buildUserDoc()));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PW)).thenReturn(true);
            when(jwtService.generateToken(USER_ID, EMAIL, "USER")).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);

            // Act
            ResponseEntity<ResponseDto> response = authService.login(loginRequest());

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatusCode()).isEqualTo(200);

            @SuppressWarnings("unchecked")
            Map<String, String> tokens = (Map<String, String>) response.getBody().getData();
            assertThat(tokens.get("accessToken")).isEqualTo(ACCESS_TOKEN);
            assertThat(tokens.get("refreshToken")).isEqualTo(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("✅ saves refresh token in Firestore after successful login")
        void savesRefreshTokenOnLogin() {
            // Arrange
            when(firestoreService.findByField("users", "email", EMAIL))
                .thenReturn(List.of(buildUserDoc()));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PW)).thenReturn(true);
            when(jwtService.generateToken(USER_ID, EMAIL, "USER")).thenReturn(ACCESS_TOKEN);
            when(jwtService.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);

            // Act
            authService.login(loginRequest());

            // Verify refresh token was persisted
            verify(refreshTokenService).saveRefreshToken(USER_ID, REFRESH_TOKEN);
        }

        @Test
        @DisplayName("❌ throws RuntimeException when email not found")
        void throwsWhenEmailNotFound() {
            // Arrange — no user with this email
            when(firestoreService.findByField("users", "email", EMAIL))
                .thenReturn(List.of());

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid email or password");

            // Verify no tokens were generated
            verify(jwtService, never()).generateToken(any(), any(), any());
            verify(refreshTokenService, never()).saveRefreshToken(any(), any());
        }

        @Test
        @DisplayName("❌ throws RuntimeException when password is wrong")
        void throwsWhenWrongPassword() {
            // Arrange — user exists but password doesn't match
            when(firestoreService.findByField("users", "email", EMAIL))
                .thenReturn(List.of(buildUserDoc()));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PW)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid email or password");

            verify(jwtService, never()).generateToken(any(), any(), any());
        }

        @Test
        @DisplayName("❌ does not leak whether email or password was wrong")
        void doesNotLeakWhichFieldFailed() {
            // Both email-not-found and wrong-password return the same message
            // to prevent email enumeration attacks

            when(firestoreService.findByField("users", "email", EMAIL))
                .thenReturn(List.of());
            String messageEmailNotFound = null;
            try {
                authService.login(loginRequest());
            } catch (RuntimeException e) {
                messageEmailNotFound = e.getMessage();
            }

            when(firestoreService.findByField("users", "email", EMAIL))
                .thenReturn(List.of(buildUserDoc()));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PW)).thenReturn(false);
            String messageWrongPassword = null;
            try {
                authService.login(loginRequest());
            } catch (RuntimeException e) {
                messageWrongPassword = e.getMessage();
            }

            // Both errors must return identical message
            assertThat(messageEmailNotFound).isEqualTo(messageWrongPassword);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // refresh()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("✅ returns new accessToken and refreshToken on success")
        void returnsNewTokensOnSuccess() {
            // Arrange
            Map<String, Object> tokenDoc = Map.of("userId", USER_ID);
            when(refreshTokenService.validateRefreshToken(REFRESH_TOKEN)).thenReturn(tokenDoc);
            when(firestoreService.findById("users", USER_ID))
                .thenReturn(Optional.of(buildUserDoc()));
            when(refreshTokenService.rotateRefreshToken(REFRESH_TOKEN, USER_ID))
                .thenReturn(NEW_REFRESH);
            when(jwtService.generateToken(USER_ID, EMAIL, "USER")).thenReturn(NEW_ACCESS);

            // Act
            ResponseEntity<ResponseDto> response = authService.refresh(REFRESH_TOKEN);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatusCode()).isEqualTo(200);

            @SuppressWarnings("unchecked")
            Map<String, String> tokens = (Map<String, String>) response.getBody().getData();
            assertThat(tokens.get("accessToken")).isEqualTo(NEW_ACCESS);
            assertThat(tokens.get("refreshToken")).isEqualTo(NEW_REFRESH);
        }

        @Test
        @DisplayName("✅ rotates the refresh token on every call")
        void rotatesRefreshToken() {
            // Arrange
            when(refreshTokenService.validateRefreshToken(REFRESH_TOKEN))
                .thenReturn(Map.of("userId", USER_ID));
            when(firestoreService.findById("users", USER_ID))
                .thenReturn(Optional.of(buildUserDoc()));
            when(refreshTokenService.rotateRefreshToken(REFRESH_TOKEN, USER_ID))
                .thenReturn(NEW_REFRESH);
            when(jwtService.generateToken(USER_ID, EMAIL, "USER")).thenReturn(NEW_ACCESS);

            // Act
            authService.refresh(REFRESH_TOKEN);

            // Verify old token was rotated
            verify(refreshTokenService).rotateRefreshToken(REFRESH_TOKEN, USER_ID);
        }

        @Test
        @DisplayName("❌ throws when refresh token is invalid")
        void throwsWhenInvalidToken() {
            when(refreshTokenService.validateRefreshToken(REFRESH_TOKEN))
                .thenThrow(new RuntimeException("Token expired or revoked"));

            assertThatThrownBy(() -> authService.refresh(REFRESH_TOKEN))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Token expired or revoked");
        }

        @Test
        @DisplayName("❌ throws when user no longer exists")
        void throwsWhenUserNotFound() {
            when(refreshTokenService.validateRefreshToken(REFRESH_TOKEN))
                .thenReturn(Map.of("userId", USER_ID));
            when(firestoreService.findById("users", USER_ID))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(REFRESH_TOKEN))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // logout()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("✅ returns HTTP 200 and revokes all tokens")
        void returns200AndRevokesTokens() {
            // Act
            ResponseEntity<ResponseDto> response = authService.logout(USER_ID);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatusCode()).isEqualTo(200);
            assertThat(response.getBody().getStatusMsg()).contains("Logged out");

            // Verify all refresh tokens were revoked
            verify(refreshTokenService).revokeAllUserTokens(USER_ID);
        }

        @Test
        @DisplayName("✅ revokes tokens exactly once per logout call")
        void revokesTokensExactlyOnce() {
            authService.logout(USER_ID);
            verify(refreshTokenService, times(1)).revokeAllUserTokens(USER_ID);
        }
    }
}