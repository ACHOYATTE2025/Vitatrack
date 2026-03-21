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

import com.portfolio.VistaTrack.Dto.ActivityRequestDto;
import com.portfolio.VistaTrack.Dto.ResponseDto;
import com.portfolio.VistaTrack.Services.ActivityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * REST controller responsible for managing user physical activities.
 * All endpoints require authentication and operate on the currently authenticated user.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/activity")
@Tag(
    name = "Activities",
    description = "API for managing user physical activities. Allows users to log, retrieve, and delete their activities."
)
public class ActivityController {

    private final ActivityService activityService;

    /**
     * Logs a new physical activity for the authenticated user.
     *
     * @param request DTO containing activity type, duration, and calories burned
     * @param userId  authenticated user's ID extracted from JWT
     * @return HTTP 201 response with confirmation and activity ID
     */
    @Operation(
        summary = "Log a new activity",
        description = "Creates a new physical activity entry for the authenticated user. " +
                      "The activity includes type, duration, and calories burned."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Activity successfully created"
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
    public ResponseEntity<ResponseDto> logActivity(
            @RequestBody @Valid ActivityRequestDto request,
            @AuthenticationPrincipal String userId) {

        log.info("[ACTIVITY CONTROLLER] POST /activity — userId: {}", userId);
        return activityService.logActivity(request, userId);
    }

    /**
     * Retrieves all activities for the authenticated user.
     *
     * @param userId authenticated user's ID extracted from JWT
     * @return HTTP 200 response with list of activities
     */
    @Operation(
        summary = "Get all user activities",
        description = "Returns all physical activities associated with the authenticated user."
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of activities retrieved successfully"
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized (missing or invalid JWT)"
    )
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getUserActivities(
            @AuthenticationPrincipal String userId) {

        log.info("[ACTIVITY CONTROLLER] GET /activity — userId: {}", userId);
        return activityService.getUserActivities(userId);
    }

    /**
     * Retrieves a specific activity by its ID.
     * Only the owner of the activity can access it.
     *
     * @param activityId activity ID
     * @param userId     authenticated user's ID extracted from JWT
     * @return HTTP 200 response with activity details
     */
    @Operation(
        summary = "Get activity by ID",
        description = "Returns a specific activity by its ID. Access is restricted to the owner of the activity."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Activity retrieved successfully"
    )
    @ApiResponse(
        responseCode = "404",
        description = "Activity not found"
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized (missing or invalid JWT)"
    )
    @GetMapping("/{activityId}")
    public ResponseEntity<Map<String, Object>> getActivityById(
            @PathVariable String activityId,
            @AuthenticationPrincipal String userId) {

        log.info("[ACTIVITY CONTROLLER] GET /activity/{} — userId: {}", activityId, userId);
        return activityService.getActivityById(activityId, userId);
    }

    /**
     * Deletes an activity by its ID.
     * Only the owner of the activity can delete it.
     *
     * @param activityId activity ID
     * @param userId     authenticated user's ID extracted from JWT
     * @return HTTP 200 response with confirmation message
     */
    @Operation(
        summary = "Delete an activity",
        description = "Deletes a specific activity by its ID. Only the owner can perform this operation."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Activity successfully deleted"
    )
    @ApiResponse(
        responseCode = "404",
        description = "Activity not found"
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized (missing or invalid JWT)"
    )
    @DeleteMapping("/{activityId}")
    public ResponseEntity<ResponseDto> deleteActivity(
            @PathVariable String activityId,
            @AuthenticationPrincipal String userId) {

        log.info("[ACTIVITY CONTROLLER] DELETE /activity/{} — userId: {}", activityId, userId);
        return activityService.deleteActivity(activityId, userId);
    }
}