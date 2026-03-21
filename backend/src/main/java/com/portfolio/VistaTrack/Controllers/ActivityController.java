package com.portfolio.VistaTrack.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.portfolio.VistaTrack.Dto.ActivityRequestDto;
import com.portfolio.VistaTrack.Dto.ResponseDto;
import com.portfolio.VistaTrack.Services.ActivityService;

import java.util.List;
import java.util.Map;

/**
 * REST controller for activity tracking.
 * Activities are nested under their parent health metric.
 *
 * Routes:
 *   POST   /health/{metricId}/activities           → log an activity under a metric
 *   GET    /health/{metricId}/activities           → list all activities for a metric
 *   DELETE /health/{metricId}/activities/{actId}  → delete an activity
 */
@Slf4j
@RestController
@RequestMapping("/health/{metricId}/activities")
@RequiredArgsConstructor
@Tag(
    name = "Activities",
    description = "Physical activity logs — nested under their parent health metric session."
)
public class ActivityController {

    private final ActivityService activityService;

    /**
     * Logs a new physical activity under an existing health metric.
     *
     * @param metricId the parent health metric ID
     * @param request  activity details (type, duration, calories)
     * @param userId   authenticated user ID from JWT
     */
    @Operation(summary = "Log a new activity under a health metric",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<ResponseDto> logActivity(
            @PathVariable String metricId,
            @RequestBody @Valid ActivityRequestDto request,
            @AuthenticationPrincipal String userId) {

        log.info("[ACTIVITY CONTROLLER] POST /health/{}/activities — userId: {}", metricId, userId);
        return activityService.logActivity(request, metricId, userId);
    }

    /**
     * Returns all activities logged under a specific health metric.
     *
     * @param metricId the parent health metric ID
     * @param userId   authenticated user ID from JWT
     */
    @Operation(summary = "Get all activities for a health metric",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getActivities(
            @PathVariable String metricId,
            @AuthenticationPrincipal String userId) {

        log.info("[ACTIVITY CONTROLLER] GET /health/{}/activities — userId: {}", metricId, userId);
        return activityService.getActivitiesForMetric(metricId, userId);
    }

    /**
     * Deletes a specific activity from a health metric.
     *
     * @param metricId   the parent health metric ID
     * @param activityId the activity document ID
     * @param userId     authenticated user ID from JWT
     */
    @Operation(summary = "Delete an activity from a health metric",
               security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{activityId}")
    public ResponseEntity<ResponseDto> deleteActivity(
            @PathVariable String metricId,
            @PathVariable String activityId,
            @AuthenticationPrincipal String userId) {

        log.info("[ACTIVITY CONTROLLER] DELETE /health/{}/activities/{} — userId: {}",
                metricId, activityId, userId);
        return activityService.deleteActivity(metricId, activityId, userId);
    }
}