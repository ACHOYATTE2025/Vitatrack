package com.portfolio.VistaTrack.Controllers;



import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.portfolio.VistaTrack.Dto.LoginRequestDto;
import com.portfolio.VistaTrack.Dto.ResponseDto;
import com.portfolio.VistaTrack.Dto.SignupRequestDto;
import com.portfolio.VistaTrack.Services.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * REST controller responsible for authentication operations in the VitaTrack Health application.
 * Provides public endpoints for user registration and authentication (no prior authentication required).
 */
@Slf4j
@Tag(
    name = "Authentication",
    description = "Authentication API for the VitaTrack Health application. Handles user registration and login operations."
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@CrossOrigin(origins = "${frontend.url:http://localhost:5173}")
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user account.
     *
     * <p>Validates the request payload, encodes the password,
     * assigns the default USER role, and persists the user in the database.</p>
     *
     * @param request DTO containing user email, username, and password
     * @return HTTP 201 response with a success message
     */
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account in the system. " +
                      "The password is securely encoded and the USER role is assigned by default."
    )
    @ApiResponse(
        responseCode = "201",
        description = "User successfully registered"
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid input data (validation error or malformed request)"
    )
    @ApiResponse(
        responseCode = "409",
        description = "User already exists with the given email"
    )
    @PostMapping("/register")
    public ResponseEntity<ResponseDto> register(@RequestBody @Valid SignupRequestDto request) {
        log.info("[AUTH CONTROLLER] POST /register — email: {}", request.getEmail());
        return authService.register(request);
    }

    // ── LOGIN ──────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Log in and receive access + refresh tokens",
        description = "Authenticates the user against Firestore. " +
                      "Returns a short-lived access token (15 min) and a long-lived refresh token (7 days). " +
                      "The refresh token is stored in Firestore and must be kept secure by the client."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login successful — both tokens returned",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {
                      "status": 200,
                      "message": "Login successful",
                      "data": {
                        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                        "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation failed — missing or malformed fields",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {
                      "status": 400,
                      "message": "Email must be a valid address",
                      "data": ""
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Invalid email or password",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {
                      "status": 500,
                      "message": "Invalid email or password",
                      "data": ""
                    }
                    """)
            )
        )
    })
    @PostMapping(
        value = "/login",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResponseDto> login(@RequestBody  @Valid LoginRequestDto request) {

        log.info("[AUTH CONTROLLER] POST /auth/login — email: {}", request.getEmail());
        return authService.login(request);
    }

    // ── REFRESH ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Refresh the access token",
        description = "Exchanges a valid refresh token for a new access token (15 min) " +
                      "and a new refresh token (7 days). " +
                      "The old refresh token is immediately revoked after use (token rotation). " +
                      "If a revoked token is reused, ALL tokens for that user are revoked as a security measure."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Tokens refreshed successfully — use the new pair going forward",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {
                      "status": 200,
                      "message": "Tokens refreshed successfully",
                      "data": {
                        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                        "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Refresh token field is missing from the request body",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {
                      "status": 400,
                      "message": "Refresh token is required",
                      "data": ""
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Token is invalid, expired, or has already been revoked",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {
                      "status": 500,
                      "message": "Refresh token has expired — please log in again",
                      "data": ""
                    }
                    """)
            )
        )
    })
    @PostMapping(
        value = "/refresh",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResponseDto> refresh(@RequestBody Map<String, String> body) {

        String refreshToken = body.get("refreshToken");

        // Validate presence before delegating to the service
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("[AUTH CONTROLLER] POST /auth/refresh — missing refresh token");
            throw new RuntimeException("Refresh token is required");
        }

        log.info("[AUTH CONTROLLER] POST /auth/refresh");
        return authService.refresh(refreshToken);
    }

    // ── LOGOUT ─────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Log out the authenticated user",
        description = "Revokes all refresh tokens belonging to the authenticated user in Firestore. " +
                      "The current access token will continue to work until it expires naturally (15 min). " +
                      "To immediately block access, the client must also discard the access token locally.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Logout successful — all refresh tokens revoked",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {
                      "status": 200,
                      "message": "Logged out successfully",
                      "data": ""
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid access token",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {
                      "status": 401,
                      "message": "Access token is missing or invalid",
                      "data": ""
                    }
                    """)
            )
        )
    })
    @PostMapping(
        value = "/logout",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResponseDto> logout(
            @AuthenticationPrincipal String userId) {

        log.info("[AUTH CONTROLLER] POST /auth/logout — userId: {}", userId);
        return authService.logout(userId);
    }
}