package com.portfolio.VistaTrack.Services;

import com.google.cloud.firestore.Firestore;
import com.portfolio.VistaTrack.Dto.ResponseDto;
import com.portfolio.VistaTrack.Exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Generates AI-powered health recommendations by sending the user's latest
 * health metrics AND their associated activities to the Groq API.
 *
 * <p>Flow:
 * <ol>
 *   <li>Fetch the user's 3 most recent health metrics from Firestore</li>
 *   <li>For each metric, fetch its activities from the subcollection
 *       health_metrics/{metricId}/activities/</li>
 *   <li>Build a rich prompt combining both metrics and activities</li>
 *   <li>Call the Groq API (llama-3.3-70b-versatile)</li>
 *   <li>Save the recommendation in Firestore recommendations/</li>
 *   <li>Return the result to the controller</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final FirestoreService firestoreService;
    private final Firestore        firestore;         // injected for subcollection access
    private final WebClient        webClient;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.api.model}")
    private String groqModel;

    private static final String HEALTH_COLLECTION          = "health_metrics";
    private static final String ACTIVITIES_SUBCOLLECTION   = "activities";
    private static final String RECOMMENDATIONS_COLLECTION = "recommendations";

    // ── GENERATE RECOMMENDATION ────────────────────────────────────────────────

    /**
     * Generates a personalized health recommendation based on the user's
     * most recent health metrics and their associated physical activities.
     *
     * @param userId the authenticated user's ID extracted from the JWT
     * @return ResponseEntity with HTTP 200 and the generated recommendation
     * @throws RuntimeException if no health metrics exist or the Groq API call fails
     */
    public ResponseEntity<ResponseDto> generateRecommendation(String userId) {
        log.info("[RECOMMENDATION] Generating recommendation for userId: {}", userId);

        // ── 1. Fetch the user's recent health metrics ──────────────────────────
        List<Map<String, Object>> metrics = firestoreService.findByField(
                HEALTH_COLLECTION, "userId", userId
        );

        if (metrics.isEmpty()) {
            log.warn("[RECOMMENDATION] No health metrics found for userId: {}", userId);
            throw new ResourceNotFoundException(
                "No health metrics found. Please record at least one health entry first."
            );
        }

        // Sort by recordedAt descending — take the 3 most recent entries
        metrics.sort((a, b) -> {
            String ta = (String) a.getOrDefault("recordedAt", "");
            String tb = (String) b.getOrDefault("recordedAt", "");
            return tb.compareTo(ta);
        });
        List<Map<String, Object>> recent = metrics.subList(0, Math.min(3, metrics.size()));
        log.debug("[RECOMMENDATION] Using {} metric(s) for prompt", recent.size());

        // ── 2. Fetch activities from each metric's subcollection ───────────────
        // For each health metric, load its associated activities from:
        // health_metrics/{metricId}/activities/
        Map<String, List<Map<String, Object>>> activitiesPerMetric = new HashMap<>();

        for (Map<String, Object> metric : recent) {
            String metricId = (String) metric.get("id");
            try {
                List<Map<String, Object>> acts = firestore
                        .collection(HEALTH_COLLECTION)
                        .document(metricId)
                        .collection(ACTIVITIES_SUBCOLLECTION)
                        .get().get()
                        .getDocuments()
                        .stream()
                        .map(doc -> (Map<String, Object>) doc.getData())
                        .toList();

                activitiesPerMetric.put(metricId, acts);
                log.debug("[RECOMMENDATION] Found {} activity/activities for metricId: {}",
                        acts.size(), metricId);

            } catch (InterruptedException | ExecutionException e) {
                // Non-blocking — if activities can't be fetched, continue with metrics only
                log.warn("[RECOMMENDATION] Could not fetch activities for metricId: {} — {}",
                        metricId, e.getMessage());
                activitiesPerMetric.put(metricId, Collections.emptyList());
            }
        }

        // ── 3. Build the prompt with both metrics and activities ───────────────
        String prompt = buildHealthPrompt(recent, activitiesPerMetric);
        log.debug("[RECOMMENDATION] Prompt built — length: {} chars", prompt.length());

        // ── 4. Call Groq API ───────────────────────────────────────────────────
        String recommendation = callGroqApi(prompt);
        log.info("[RECOMMENDATION] Received recommendation from Groq for userId: {}", userId);

        // ── 5. Save recommendation in Firestore ────────────────────────────────
        String recommendationId = UUID.randomUUID().toString();

        // Count total activities used across all metrics
        int totalActivities = activitiesPerMetric.values().stream()
                .mapToInt(List::size).sum();

        Map<String, Object> doc = new HashMap<>();
        doc.put("id",               recommendationId);
        doc.put("userId",           userId);
        doc.put("recommendation",   recommendation);
        doc.put("basedOnMetrics",   recent.size());
        doc.put("basedOnActivities", totalActivities);
        doc.put("generatedAt",      Instant.now().toString());

        firestoreService.save(RECOMMENDATIONS_COLLECTION, recommendationId, doc);
        log.info("[RECOMMENDATION] Recommendation saved — id: {} | metrics: {} | activities: {}",
                recommendationId, recent.size(), totalActivities);

        // ── 6. Return to controller ────────────────────────────────────────────
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("recommendationId",  recommendationId);
        responseData.put("recommendation",    recommendation);
        responseData.put("basedOnMetrics",    recent.size());
        responseData.put("basedOnActivities", totalActivities);
        responseData.put("generatedAt",       doc.get("generatedAt"));

        return ResponseEntity.ok(
                new ResponseDto(200, "Recommendation generated successfully", responseData));
    }

    // ── GET RECOMMENDATION HISTORY ─────────────────────────────────────────────

    /**
     * Returns all past recommendations generated for the authenticated user,
     * sorted by most recent first.
     *
     * @param userId the authenticated user's ID
     * @return ResponseEntity with the list of past recommendations
     */
    public ResponseEntity<List<Map<String, Object>>> getRecommendations(String userId) {
        log.info("[RECOMMENDATION] Fetching recommendation history for userId: {}", userId);

        List<Map<String, Object>> recommendations = firestoreService.findByField(
                RECOMMENDATIONS_COLLECTION, "userId", userId
        );

        // Sort by generatedAt descending — most recent first
        recommendations.sort((a, b) -> {
            String ta = (String) a.getOrDefault("generatedAt", "");
            String tb = (String) b.getOrDefault("generatedAt", "");
            return tb.compareTo(ta);
        });

        log.info("[RECOMMENDATION] Found {} recommendation(s) for userId: {}",
                recommendations.size(), userId);
        return ResponseEntity.ok(recommendations);
    }

    // ── PRIVATE: BUILD PROMPT ──────────────────────────────────────────────────

    /**
     * Builds a rich prompt combining the user's health metrics and their
     * associated physical activities for each session.
     *
     * <p>For each metric entry, lists the health data followed by the
     * activities performed during that same session — giving Groq full
     * context to generate relevant, personalized recommendations.</p>
     *
     * @param metrics             list of recent health metric documents
     * @param activitiesPerMetric map of metricId → list of activity documents
     * @return formatted prompt string ready to send to Groq
     */
    private String buildHealthPrompt(
            List<Map<String, Object>> metrics,
            Map<String, List<Map<String, Object>>> activitiesPerMetric) {

        StringBuilder sb = new StringBuilder();

        sb.append("You are a professional health and fitness advisor. ");
        sb.append("Based on the following health metrics and physical activities, ");
        sb.append("provide 3 specific, actionable, and personalized recommendations. ");
        sb.append("Be concise, encouraging, and practical. ");
        sb.append("Format your response as 3 numbered recommendations.\n\n");
        sb.append("Health sessions (most recent first):\n");

        for (int i = 0; i < metrics.size(); i++) {
            Map<String, Object> m = metrics.get(i);
            String metricId = (String) m.get("id");

            sb.append("\n--- Session ").append(i + 1).append(" ---\n");

            // Health metrics — append only non-null values
            if (m.get("recordedAt") != null)
                sb.append("Recorded: ").append(m.get("recordedAt")).append("\n");
            if (m.get("weightKg") != null)
                sb.append("Weight: ").append(m.get("weightKg")).append(" kg\n");
            if (m.get("heightCm") != null)
                sb.append("Height: ").append(m.get("heightCm")).append(" cm\n");
            if (m.get("bmi") != null)
                sb.append("BMI: ").append(m.get("bmi")).append("\n");
            if (m.get("heartRate") != null)
                sb.append("Heart rate: ").append(m.get("heartRate")).append(" bpm\n");
            if (m.get("systolic") != null)
                sb.append("Blood pressure: ")
                  .append(m.get("systolic")).append("/")
                  .append(m.get("diastolic")).append(" mmHg\n");
            if (m.get("notes") != null)
                sb.append("Notes: ").append(m.get("notes")).append("\n");

            // Physical activities linked to this metric session
            List<Map<String, Object>> acts =
                    activitiesPerMetric.getOrDefault(metricId, Collections.emptyList());

            if (acts.isEmpty()) {
                sb.append("Physical activities: none recorded for this session\n");
            } else {
                sb.append("Physical activities during this session:\n");
                for (Map<String, Object> a : acts) {
                    sb.append("  - ");
                    if (a.get("type")            != null)
                        sb.append(a.get("type")).append(", ");
                    if (a.get("durationMinutes") != null)
                        sb.append(a.get("durationMinutes")).append(" min, ");
                    if (a.get("caloriesBurned")  != null)
                        sb.append(a.get("caloriesBurned")).append(" kcal burned");
                    if (a.get("notes")           != null)
                        sb.append(", notes: ").append(a.get("notes"));
                    sb.append("\n");
                }
            }
        }

        sb.append("\nProvide your 3 recommendations now:");
        return sb.toString();
    }

    // ── PRIVATE: CALL GROQ API ─────────────────────────────────────────────────

    /**
     * Sends the prompt to the Groq API and returns the model's response text.
     * The request body is built as a raw JSON string to avoid WebClient
     * type-erasure issues with nested Lists inside Maps.
     *
     * @param prompt the health + activity prompt to send
     * @return the model's recommendation as a plain string
     * @throws RuntimeException if the API call fails or returns an error
     */
    @SuppressWarnings("unchecked")
    private String callGroqApi(String prompt) {
        log.info("[RECOMMENDATION] Calling Groq API — model: {}", groqModel);

        // Escape prompt for safe JSON string embedding
        String safePrompt = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

        // Build request body as raw JSON — avoids WebClient Map/List serialization issues
        String requestBody = String.format(
            "{\"model\":\"%s\",\"temperature\":0.7,\"max_tokens\":600," +
            "\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
            groqModel, safePrompt
        );

        log.debug("[RECOMMENDATION] Request body length: {} chars", requestBody.length());

        try {
            Map<String, Object> response = webClient.post()
                    .uri(groqApiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT,       MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(errorBody -> {
                                    log.error("[RECOMMENDATION] Groq error: {}", errorBody);
                                    return new RuntimeException("Groq API error: " + errorBody);
                                })
                    )
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Groq API returned an empty response");
            }

            // Extract text from choices[0].message.content
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            log.info("[RECOMMENDATION] Groq API call successful");
            return content;

        } catch (Exception e) {
            log.error("[RECOMMENDATION] Groq API call failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate recommendation: " + e.getMessage());
        }
    }
}