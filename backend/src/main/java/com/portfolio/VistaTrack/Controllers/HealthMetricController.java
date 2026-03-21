package com.portfolio.VistaTrack.Controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.VistaTrack.Dto.HealthMetricRequestDto;
import com.portfolio.VistaTrack.Dto.ResponseDto;
import com.portfolio.VistaTrack.Services.HealthMetricService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller responsible for managing user health metrics.
 * All endpoints require authentication and operate on the currently authenticated user.
 */
@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Tag(
    name = "Health Metrics",
    description = "API for managing user health metrics such as weight, height, heart rate, and blood pressure."
)
public class HealthMetricController {

    private final HealthMetricService healthMetricService;

    /**
     * Records a new health metric entry for the authenticated user.
     *
     * @param request DTO containing weight, height, heart rate, blood pressure, and notes
     * @param userId  authenticated user's ID extracted from JWT
     * @return HTTP 201 response with confirmation and metric ID
     */
    @Operation(
        summary = "Record a new health metric",
        description = "Creates a new health metric entry for the authenticated user. " +
                      "Includes data such as weight, height, heart rate, and blood pressure."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Health metric successfully recorded"
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request payload"
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized (missing or invalid JWT)"
    )
    @PostMapping
    public ResponseEntity<ResponseDto> recordMetric(
            @RequestBody @Valid HealthMetricRequestDto request,
            @AuthenticationPrincipal String userId) {

        log.info("[HEALTH CONTROLLER] POST /health — userId: {}", userId);
        return healthMetricService.recordMetric(request, userId);
    }

    /**
     * Retrieves all health metrics for the authenticated user.
     *
     * @param userId authenticated user's ID extracted from JWT
     * @return HTTP 200 response with list of health metrics
     */
    @Operation(
        summary = "Get all health metrics",
        description = "Returns all health metrics associated with the authenticated user."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Health metrics retrieved successfully"
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized (missing or invalid JWT)"
    )
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getUserMetrics(
            @AuthenticationPrincipal String userId) {

        log.info("[HEALTH CONTROLLER] GET /health — userId: {}", userId);
        return healthMetricService.getUserMetrics(userId);
    }

    /**
     * Retrieves a specific health metric by its ID.
     * Only the owner of the metric can access it.
     *
     * @param metricId health metric ID
     * @param userId   authenticated user's ID extracted from JWT
     * @return HTTP 200 response with health metric details
     */
    @Operation(
        summary = "Get health metric by ID",
        description = "Returns a specific health metric by its ID. Access is restricted to the owner."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Health metric retrieved successfully"
    )
    @ApiResponse(
        responseCode = "404",
        description = "Health metric not found"
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized (missing or invalid JWT)"
    )
    @GetMapping("/{metricId}")
    public ResponseEntity<Map<String, Object>> getMetricById(
            @PathVariable String metricId,
            @AuthenticationPrincipal String userId) {

        log.info("[HEALTH CONTROLLER] GET /health/{} — userId: {}", metricId, userId);
        return healthMetricService.getMetricById(metricId, userId);
    }

    /**
     * Deletes a health metric entry by its ID.
     * Only the owner can perform this operation.
     *
     * @param metricId health metric ID
     * @param userId   authenticated user's ID extracted from JWT
     * @return HTTP 200 response with confirmation message
     */
    @Operation(
        summary = "Delete a health metric",
        description = "Deletes a specific health metric by its ID. Only the owner can perform this action."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Health metric successfully deleted"
    )
    @ApiResponse(
        responseCode = "404",
        description = "Health metric not found"
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized (missing or invalid JWT)"
    )
    @DeleteMapping("/{metricId}")
    public ResponseEntity<ResponseDto> deleteMetric(
            @PathVariable String metricId,
            @AuthenticationPrincipal String userId) {

        log.info("[HEALTH CONTROLLER] DELETE /health/{} — userId: {}", metricId, userId);
        return healthMetricService.deleteMetric(metricId, userId);
    }
}