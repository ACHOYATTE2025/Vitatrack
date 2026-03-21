package com.portfolio.VistaTrack.Services;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.portfolio.VistaTrack.Dto.HealthMetricRequestDto;
import com.portfolio.VistaTrack.Dto.ResponseDto;
import com.portfolio.VistaTrack.Exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthMetricService {
    private final FirestoreService firestoreService;

    private static final String HEALTH_METRICS_COLLECTION = "health_metrics";

    // ── RECORD METRIC ──────────────────────────────────────────────────────────

    /**
     * Records a new health metric entry for the authenticated user.
     *
     * <p>Steps:
     * <ol>
     *   <li>Generate a unique metric ID</li>
     *   <li>Compute BMI automatically if weight and height are provided</li>
     *   <li>Build the Firestore document from the request DTO</li>
     *   <li>Persist it in Firestore under the "health_metrics" collection</li>
     *   <li>Return HTTP 201 with a confirmation message</li>
     * </ol>
     * </p>
     *
     * @param request DTO containing weight, height, heart rate, and optional notes
     * @param userId  the authenticated user's ID extracted from the JWT
     * @return ResponseEntity with HTTP 201 and the new metric ID
     */
    public ResponseEntity<ResponseDto> recordMetric(HealthMetricRequestDto request, String userId) {
        log.info("[HEALTH] Recording new health metric for userId: {}", userId);

        // ── 1. Generate a unique ID for this metric entry ──────────────────────
        String metricId = UUID.randomUUID().toString();

        // ── 2. Auto-compute BMI if both weight and height are provided ─────────
        // BMI formula: weight(kg) / height(m)²
        Double bmi = null;
        if (request.getWeightKg() != null && request.getHeightCm() != null
                && request.getHeightCm() > 0) {
            double heightInMeters = request.getHeightCm() / 100.0;
            bmi = request.getWeightKg() / (heightInMeters * heightInMeters);
            bmi = Math.round(bmi * 100.0) / 100.0; // round to 2 decimal places
            log.debug("[HEALTH] BMI computed: {} for userId: {}", bmi, userId);
        }

        // ── 3. Build the Firestore document ────────────────────────────────────
        Map<String, Object> metricDocument = new HashMap<>();
        metricDocument.put("id",          metricId);
        metricDocument.put("userId",      userId);
        metricDocument.put("weightKg",    request.getWeightKg());
        metricDocument.put("heightCm",    request.getHeightCm());
        metricDocument.put("bmi",         bmi);                          // computed or null
        metricDocument.put("heartRate",   request.getHeartRate());       // beats per minute
        metricDocument.put("systolic",    request.getSystolic());        // blood pressure high
        metricDocument.put("diastolic",   request.getDiastolic());       // blood pressure low
        metricDocument.put("notes",       request.getNotes());           // optional free-text
        metricDocument.put("recordedAt",  Instant.now().toString());     // ISO-8601 timestamp

        // ── 4. Persist in Firestore ────────────────────────────────────────────
        firestoreService.save(HEALTH_METRICS_COLLECTION, metricId, metricDocument);
        log.info("[HEALTH] Health metric recorded successfully — metricId: {} | userId: {}",
                metricId, userId);

        // ── 5. Return 201 Created ──────────────────────────────────────────────
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto(201, "Health metric recorded successfully", metricId));
    }

    // ── GET ALL METRICS FOR A USER ─────────────────────────────────────────────

    /**
     * Retrieves all health metric entries belonging to the authenticated user.
     *
     * @param userId the authenticated user's ID extracted from the JWT
     * @return ResponseEntity with HTTP 200 and the list of metric documents
     */
    public ResponseEntity<List<Map<String, Object>>> getUserMetrics(String userId) {
        log.info("[HEALTH] Fetching all health metrics for userId: {}", userId);

        List<Map<String, Object>> metrics = firestoreService.findByField(
                HEALTH_METRICS_COLLECTION, "userId", userId
        );

        log.info("[HEALTH] Found {} metric(s) for userId: {}", metrics.size(), userId);
        return ResponseEntity.ok(metrics);
    }

    // ── GET SINGLE METRIC ──────────────────────────────────────────────────────

    /**
     * Retrieves a single health metric entry by its ID.
     * Verifies that the metric belongs to the requesting user before returning it.
     *
     * @param metricId the metric document ID to retrieve
     * @param userId   the authenticated user's ID — used to verify ownership
     * @return ResponseEntity with HTTP 200 and the metric document
     * @throws ResourceNotFoundException if the metric does not exist
     * @throws RuntimeException          if the metric belongs to a different user
     */
    public ResponseEntity<Map<String, Object>> getMetricById(String metricId, String userId) {
        log.info("[HEALTH] Fetching metric — metricId: {} | userId: {}", metricId, userId);

        // ── 1. Fetch the document from Firestore ───────────────────────────────
        Map<String, Object> metric = firestoreService.findById(HEALTH_METRICS_COLLECTION, metricId)
                .orElseThrow(() -> {
                    log.warn("[HEALTH] Metric not found — metricId: {}", metricId);
                    return new ResourceNotFoundException("Health metric not found with id: " + metricId);
                });

        // ── 2. Verify ownership ────────────────────────────────────────────────
        if (!userId.equals(metric.get("userId"))) {
            log.warn("[HEALTH] Ownership check failed — metricId: {} does not belong to userId: {}",
                    metricId, userId);
            throw new RuntimeException("Access denied — this metric does not belong to you");
        }

        log.info("[HEALTH] Metric retrieved successfully — metricId: {}", metricId);
        return ResponseEntity.ok(metric);
    }

    // ── DELETE METRIC ──────────────────────────────────────────────────────────

    /**
     * Deletes a health metric entry by its ID.
     * Verifies ownership before deleting — users can only delete their own data.
     *
     * @param metricId the metric document ID to delete
     * @param userId   the authenticated user's ID — used to verify ownership
     * @return ResponseEntity with HTTP 200 and a confirmation message
     * @throws ResourceNotFoundException if the metric does not exist
     * @throws RuntimeException          if the metric belongs to a different user
     */
    public ResponseEntity<ResponseDto> deleteMetric(String metricId, String userId) {
        log.info("[HEALTH] Delete request — metricId: {} | userId: {}", metricId, userId);

        // ── 1. Verify the metric exists ────────────────────────────────────────
        Map<String, Object> metric = firestoreService.findById(HEALTH_METRICS_COLLECTION, metricId)
                .orElseThrow(() -> {
                    log.warn("[HEALTH] Delete failed — metric not found: {}", metricId);
                    return new ResourceNotFoundException("Health metric not found with id: " + metricId);
                });

        // ── 2. Verify ownership before deleting ────────────────────────────────
        if (!userId.equals(metric.get("userId"))) {
            log.warn("[HEALTH] Delete denied — metricId: {} does not belong to userId: {}",
                    metricId, userId);
            throw new RuntimeException("Access denied — this metric does not belong to you");
        }

        // ── 3. Delete from Firestore ───────────────────────────────────────────
        firestoreService.delete(HEALTH_METRICS_COLLECTION, metricId);
        log.info("[HEALTH] Metric deleted successfully — metricId: {}", metricId);

        return ResponseEntity.ok(new ResponseDto(200, "Health metric deleted successfully", ""));
    }

}
