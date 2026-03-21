package com.portfolio.VistaTrack.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO carrying the login payload sent by the client.
 * Contains only email and password — no username needed for authentication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
     /**
     * The user's registered email address — used as the login identifier.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    private String email;

    /**
     * The user's raw password — compared against the BCrypt hash stored in Firestore.
     */
    @NotBlank(message = "Password is required")
    private String password;

}
