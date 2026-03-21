package com.portfolio.VistaTrack.Dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
@Getter
public class SignupRequestDto {

    /**
     * The user's chosen display name.
     * Must be between 3 and 50 characters.
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /**
     * The user's email address — used as the unique login identifier.
     * Must follow a valid email format.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address (e.g. user@example.com)")
    private String email;

    /**
     * The user's raw password — will be encoded before persistence.
     * Must be at least 8 characters long.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
}
