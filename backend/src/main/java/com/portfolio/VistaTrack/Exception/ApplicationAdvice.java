package com.portfolio.VistaTrack.Exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.portfolio.VistaTrack.Dto.ErroResponseDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class ApplicationAdvice {

    /**
     * Catches any unhandled {@link Exception} thrown across the entire application.
     *
     * <p>Builds a standardized {@link ErroResponseDto} containing the request path,
     * HTTP status, error message, and timestamp, then returns it with HTTP 400.</p>
     *
     * @param exception  the exception that was thrown
     * @param webRequest the current web request (used to extract the request path)
     * @return ResponseEntity wrapping the error DTO with HTTP 400 Bad Request
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponseDto> exceptionHandler(Exception exception, WebRequest webRequest) {

        // Extract the request path for context (e.g. "uri=/api/register")
        String requestPath = webRequest.getDescription(false);

        log.error("[EXCEPTION HANDLER] Unhandled exception caught — path: {} | message: {}",
                requestPath, exception.getMessage(), exception);

        // Build the standardized error response payload
        ErroResponseDto errorDto = new ErroResponseDto(
                requestPath,
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(errorDto);
    }
}
