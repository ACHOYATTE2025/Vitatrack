package com.portfolio.VistaTrack.Services;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.portfolio.VistaTrack.Dto.ActivityRequestDto;
import com.portfolio.VistaTrack.Dto.ResponseDto;
import com.portfolio.VistaTrack.Exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles all activity logging business logic for the VitaTrack application.
 *
 * Responsibilities:
 * 
 *   Log a new activity for the authenticated user
 *   Retrieve all activities belonging to a specific user
 *   Retrieve a single activity by its ID
 *   Delete an activity owned by the authenticated user
 * 
 * 
 *
 * All data is persisted in the Firestore "activities" collection.
 * Each document is keyed by a generated UUID and contains a userId field
 * linking it back to the user who logged it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final FirestoreService firestoreService;

    private static final String ACTIVITIES_COLLECTION = "activities";

    // ── LOG ACTIVITY ───────────────────────────────────────────────────────────

    /**
     * Logs a new physical activity for the authenticated user.
     *
     * Steps:
     * 
     *   Generate a unique activity ID
     *   Build the Firestore document from the request DTO
     *   Persist it in Firestore under the "activities" collection
     *   Return HTTP 201 with a confirmation message
     * 
     * 
     *
     * @param request DTO containing activity type, duration, and calories
     * @param userId  the authenticated user's ID extracted from the JWT
     * @return ResponseEntity with HTTP 201 and a confirmation message
     */
    public ResponseEntity<ResponseDto> logActivity(ActivityRequestDto request, String userId) {
        log.info("[ACTIVITY] Logging new activity for userId: {} | type: {}",
                userId, request.getType());

        // ── 1. Generate a unique ID for this activity ──────────────────────────
        String activityId = UUID.randomUUID().toString();

        // ── 2. Build the Firestore document ────────────────────────────────────
        Map<String, Object> activityDocument = new HashMap<>();
        activityDocument.put("id",              activityId);
        activityDocument.put("userId",          userId);               // links to the user
        activityDocument.put("type",            request.getType());    // e.g. running, cycling
        activityDocument.put("durationMinutes", request.getDurationMinutes());
        activityDocument.put("caloriesBurned",  request.getCaloriesBurned());
        activityDocument.put("notes",           request.getNotes());   // optional free-text
        activityDocument.put("loggedAt",        Instant.now().toString()); // ISO-8601 timestamp

        // ── 3. Persist in Firestore ────────────────────────────────────────────
        firestoreService.save(ACTIVITIES_COLLECTION, activityId, activityDocument);
        log.info("[ACTIVITY] Activity logged successfully — activityId: {} | userId: {}",
                activityId, userId);

        // ── 4. Return 201 Created ──────────────────────────────────────────────
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto(201, "Activity logged successfully", activityId));
    }

    // ── GET ALL ACTIVITIES FOR A USER ──────────────────────────────────────────

    /**
     * Retrieves all activity logs belonging to the authenticated user.
     *
     * @param userId the authenticated user's ID extracted from the JWT
     * @return ResponseEntity with HTTP 200 and the list of activity documents
     */
    public ResponseEntity<List<Map<String, Object>>> getUserActivities(String userId) {
        log.info("[ACTIVITY] Fetching all activities for userId: {}", userId);

        // Query Firestore for all documents where userId matches
        List<Map<String, Object>> activities = firestoreService.findByField(
                ACTIVITIES_COLLECTION, "userId", userId
        );

        log.info("[ACTIVITY] Found {} activity/activities for userId: {}",
                activities.size(), userId);

        return ResponseEntity.ok(activities);
    }

    // ── GET SINGLE ACTIVITY ────────────────────────────────────────────────────

    /**
     * Retrieves a single activity by its ID.
     * Verifies that the activity belongs to the requesting user before returning it.
     *
     * @param activityId the activity document ID to retrieve
     * @param userId     the authenticated user's ID — used to verify ownership
     * @return ResponseEntity with HTTP 200 and the activity document
     * @throws ResourceNotFoundException if the activity does not exist
     * @throws RuntimeException          if the activity belongs to a different user
     */
    public ResponseEntity<Map<String, Object>> getActivityById(String activityId, String userId) {
        log.info("[ACTIVITY] Fetching activity — activityId: {} | userId: {}", activityId, userId);

        // ── 1. Fetch the document from Firestore ───────────────────────────────
        Map<String, Object> activity = firestoreService.findById(ACTIVITIES_COLLECTION, activityId)
                .orElseThrow(() -> {
                    log.warn("[ACTIVITY] Activity not found — activityId: {}", activityId);
                    return new ResourceNotFoundException("Activity not found with id: " + activityId);
                });

        // ── 2. Verify ownership — users can only access their own data ─────────
        if (!userId.equals(activity.get("userId"))) {
            log.warn("[ACTIVITY] Ownership check failed — activityId: {} does not belong to userId: {}",
                    activityId, userId);
            throw new RuntimeException("Access denied — this activity does not belong to you");
        }

        log.info("[ACTIVITY] Activity retrieved successfully — activityId: {}", activityId);
        return ResponseEntity.ok(activity);
    }


    // ── DELETE ACTIVITY ────────────────────────────────────────────────────────

    /**
     * Deletes an activity by its ID.
     * Verifies ownership before deleting — users can only delete their own activities.
     *
     * @param activityId the activity document ID to delete
     * @param userId     the authenticated user's ID — used to verify ownership
     * @return ResponseEntity with HTTP 200 and a confirmation message
     * @throws ResourceNotFoundException if the activity does not exist
     * @throws RuntimeException          if the activity belongs to a different user
     */
    public ResponseEntity<ResponseDto> deleteActivity(String activityId, String userId) {
        log.info("[ACTIVITY] Delete request — activityId: {} | userId: {}", activityId, userId);

        // ── 1. Verify the activity exists ──────────────────────────────────────
        Map<String, Object> activity = firestoreService.findById(ACTIVITIES_COLLECTION, activityId)
                .orElseThrow(() -> {
                    log.warn("[ACTIVITY] Delete failed — activity not found: {}", activityId);
                    return new ResourceNotFoundException("Activity not found with id: " + activityId);
                });

        // ── 2. Verify ownership before deleting ────────────────────────────────
        if (!userId.equals(activity.get("userId"))) {
            log.warn("[ACTIVITY] Delete denied — activityId: {} does not belong to userId: {}",
                    activityId, userId);
            throw new RuntimeException("Access denied — this activity does not belong to you");
        }

        // ── 3. Delete from Firestore ───────────────────────────────────────────
        firestoreService.delete(ACTIVITIES_COLLECTION, activityId);
        log.info("[ACTIVITY] Activity deleted successfully — activityId: {}", activityId);

        return ResponseEntity.ok(new ResponseDto(200, "Activity deleted successfully", ""));
    }
}
