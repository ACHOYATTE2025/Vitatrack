package com.portfolio.VistaTrack.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested resource cannot be found in the system.
 *
 * <p>Automatically maps to HTTP 404 Not Found via {@link ResponseStatus}.
 * Use this instead of a generic {@link RuntimeException} whenever an entity
 * lookup returns no result (e.g. user not found, role not found, record missing).</p>
 *
 * <p>Example usage:</p>
 * <pre>
 *     userRepository.findById(id)
 *         .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
 * </pre>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code ResourceNotFoundException} with the given detail message.
     *
     * @param message a descriptive message identifying which resource was not found
     *                (e.g. "User not found with email: john@example.com")
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
