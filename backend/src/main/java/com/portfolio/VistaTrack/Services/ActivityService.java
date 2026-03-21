package com.portfolio.VistaTrack.Services;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.portfolio.VistaTrack.Dto.ActivityRequestDto;
import com.portfolio.VistaTrack.Dto.ResponseDto;
import com.portfolio.VistaTrack.Exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * Handles all activity business logic for VitaTrack.
 *
 * Activities are stored as a subcollection of their parent health metric:
 *   health_metrics/{metricId}/activities/{activityId}
 *
 * This reflects the 1-to-many relationship: one health session can have
 * multiple associated physical activities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final com.google.cloud.firestore.Firestore firestore;

    private static final String METRICS_COLLECTION    = "health_metrics";
    private static final String ACTIVITIES_SUBCOLLECTION = "activities";

    // ── LOG ACTIVITY ───────────────────────────────────────────────────────────

    /**
     * Logs a new physical activity under an existing health metric.
     *
     * The activity is stored as a document in the subcollection:
     * health_metrics/{metricId}/activities/{activityId}
     *
     * @param request  DTO containing activity type, duration, and calories
     * @param metricId the parent health metric this activity belongs to
     * @param userId   the authenticated user's ID — used to verify metric ownership
     * @return ResponseEntity with HTTP 201 and the new activity ID
     */
    public ResponseEntity<ResponseDto> logActivity(
            ActivityRequestDto request, String metricId, String userId) {

        log.info("[ACTIVITY] Logging activity for metricId: {} | userId: {}", metricId, userId);

        try {
            // ── 1. Verify the parent metric exists and belongs to this user ──────
            var metricDoc = firestore.collection(METRICS_COLLECTION)
                    .document(metricId).get().get();

            if (!metricDoc.exists()) {
                log.warn("[ACTIVITY] Parent metric not found — metricId: {}", metricId);
                throw new ResourceNotFoundException("Health metric not found with id: " + metricId);
            }

            String ownerUserId = (String) metricDoc.getData().get("userId");
            if (!userId.equals(ownerUserId)) {
                log.warn("[ACTIVITY] Ownership check failed for metricId: {}", metricId);
                throw new RuntimeException("Access denied — this metric does not belong to you");
            }

            // ── 2. Build the activity document ─────────────────────────────────
            String activityId = UUID.randomUUID().toString();
            Map<String, Object> activityDoc = new HashMap<>();
            activityDoc.put("id",              activityId);
            activityDoc.put("metricId",        metricId);       // parent reference
            activityDoc.put("userId",          userId);
            activityDoc.put("type",            request.getType());
            activityDoc.put("durationMinutes", request.getDurationMinutes());
            activityDoc.put("caloriesBurned",  request.getCaloriesBurned());
            activityDoc.put("notes",           request.getNotes());
            activityDoc.put("loggedAt",        Instant.now().toString());

            // ── 3. Save in subcollection health_metrics/{metricId}/activities/ ──
            firestore.collection(METRICS_COLLECTION)
                    .document(metricId)
                    .collection(ACTIVITIES_SUBCOLLECTION)
                    .document(activityId)
                    .set(activityDoc)
                    .get();

            log.info("[ACTIVITY] Activity logged — activityId: {} under metricId: {}",
                    activityId, metricId);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ResponseDto(201, "Activity logged successfully", activityId));

        } catch (InterruptedException | ExecutionException e) {
            log.error("[ACTIVITY] Failed to log activity: {}", e.getMessage());
            throw new RuntimeException("Failed to log activity: " + e.getMessage());
        }
    }

    // ── GET ALL ACTIVITIES FOR A METRIC ────────────────────────────────────────

    /**
     * Returns all activities logged under a specific health metric.
     *
     * @param metricId the parent health metric ID
     * @param userId   the authenticated user's ID — used to verify ownership
     * @return ResponseEntity with HTTP 200 and the list of activities
     */
    public ResponseEntity<List<Map<String, Object>>> getActivitiesForMetric(
            String metricId, String userId) {

        log.info("[ACTIVITY] Fetching activities for metricId: {} | userId: {}", metricId, userId);

        try {
            // Verify ownership of the parent metric
            var metricDoc = firestore.collection(METRICS_COLLECTION)
                    .document(metricId).get().get();

            if (!metricDoc.exists()) {
                throw new ResourceNotFoundException("Health metric not found: " + metricId);
            }
            if (!userId.equals(metricDoc.getData().get("userId"))) {
                throw new RuntimeException("Access denied");
            }

            // Fetch all documents from the subcollection
            var activities = firestore.collection(METRICS_COLLECTION)
                    .document(metricId)
                    .collection(ACTIVITIES_SUBCOLLECTION)
                    .get().get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.getData())
                    .toList();

            log.info("[ACTIVITY] Found {} activity/activities for metricId: {}",
                    activities.size(), metricId);
            return ResponseEntity.ok(activities);

        } catch (InterruptedException | ExecutionException e) {
            log.error("[ACTIVITY] Failed to fetch activities: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch activities: " + e.getMessage());
        }
    }

    // ── DELETE ACTIVITY ────────────────────────────────────────────────────────

    /**
     * Deletes a specific activity from a health metric's subcollection.
     *
     * @param metricId   the parent health metric ID
     * @param activityId the activity document ID to delete
     * @param userId     the authenticated user's ID
     * @return ResponseEntity with HTTP 200 and a confirmation message
     */
    public ResponseEntity<ResponseDto> deleteActivity(
            String metricId, String activityId, String userId) {

        log.info("[ACTIVITY] Delete request — activityId: {} under metricId: {}", activityId, metricId);

        try {
            // Verify the activity exists in the subcollection
            var activityDoc = firestore.collection(METRICS_COLLECTION)
                    .document(metricId)
                    .collection(ACTIVITIES_SUBCOLLECTION)
                    .document(activityId)
                    .get().get();

            if (!activityDoc.exists()) {
                throw new ResourceNotFoundException("Activity not found: " + activityId);
            }
            if (!userId.equals(activityDoc.getData().get("userId"))) {
                throw new RuntimeException("Access denied");
            }

            // Delete from subcollection
            firestore.collection(METRICS_COLLECTION)
                    .document(metricId)
                    .collection(ACTIVITIES_SUBCOLLECTION)
                    .document(activityId)
                    .delete().get();

            log.info("[ACTIVITY] Activity deleted — activityId: {}", activityId);
            return ResponseEntity.ok(new ResponseDto(200, "Activity deleted successfully", null));

        } catch (InterruptedException | ExecutionException e) {
            log.error("[ACTIVITY] Failed to delete activity: {}", e.getMessage());
            throw new RuntimeException("Failed to delete activity: " + e.getMessage());
        }
    }
}