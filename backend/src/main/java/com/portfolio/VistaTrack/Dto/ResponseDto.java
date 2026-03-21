package com.portfolio.VistaTrack.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



/**
 * Standard API response wrapper used by all endpoints.
 *
 * @param status  HTTP status code (e.g. 200, 201, 400)
 * @param message human-readable description of the result
 * @param data    optional payload — JWT token on login, empty string otherwise
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDto {


    
  private int statusCode;

  private String statusMsg;

   private Object data;
}
