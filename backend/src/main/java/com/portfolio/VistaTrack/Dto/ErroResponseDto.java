package com.portfolio.VistaTrack.Dto;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
  @Schema(
        name = "ErrorResponse",
        description = "Schema to hold error response information")
public class ErroResponseDto {
      @Schema(
            description = "API path invoked by User"
    )
  private String apiPath;

  
   @Schema(
            description = "Error code representing the error happened"
    )
  private HttpStatus errorCode;


 @Schema(
            description = "Error message representing the error happened"
    )
  private String errorMessage;

   @Schema(
            description = "Time representing when the error happened"
    )
  private LocalDateTime errorTime;
}
