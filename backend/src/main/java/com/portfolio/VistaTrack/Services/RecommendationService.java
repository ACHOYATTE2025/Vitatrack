package com.portfolio.VistaTrack.Services;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.portfolio.VistaTrack.Dto.ResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final FirestoreService firestoreService;
    private final WebClient        webClient;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.api.model}")
    private String groqModel;

    private static final String HEALTH_COLLECTION          = "health_metrics";
    private static final String RECOMMENDATIONS_COLLECTION = "recommendations";

   

    // ── GENERATE RECOMMENDATION ────────────────────────────────────────────────

    /**
     * Generates a personalized health recommendation based on the user's
     * most recent health metrics stored in Firestore.
     *
     * @param userId the authenticated user's ID
     * @return ResponseEntity with the generated recommendation text
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
            throw new RuntimeException(
                "No health metrics found. Please record at least one health entry first."
            );
        }

        // Use the most recent entry (sort by recordedAt descending, take last 3)
        metrics.sort((a, b) -> {
            String ta = (String) a.getOrDefault("recordedAt", "");
            String tb = (String) b.getOrDefault("recordedAt", "");
            return tb.compareTo(ta);
        });
        List<Map<String, Object>> recent = metrics.subList(0, Math.min(3, metrics.size()));
        log.debug("[RECOMMENDATION] Using {} metric(s) for prompt", recent.size());

        // ── 2. Build the prompt from the metrics ───────────────────────────────
        String prompt = buildHealthPrompt(recent);
        log.debug("[RECOMMENDATION] Prompt built — length: {} chars", prompt.length());

        // ── 3. Call Groq API ───────────────────────────────────────────────────
        String recommendation = callGroqApi(prompt);
        log.info("[RECOMMENDATION] Received recommendation from Groq for userId: {}", userId);

        // ── 4. Save recommendation in Firestore ────────────────────────────────
        String recommendationId = UUID.randomUUID().toString();
        Map<String, Object> doc = new HashMap<>();
        doc.put("id",             recommendationId);
        doc.put("userId",         userId);
        doc.put("recommendation", recommendation);
        doc.put("basedOnMetrics", recent.size());
        doc.put("generatedAt",    Instant.now().toString());

        firestoreService.save(RECOMMENDATIONS_COLLECTION, recommendationId, doc);
        log.info("[RECOMMENDATION] Recommendation saved — id: {}", recommendationId);

        // ── 5. Return to controller ────────────────────────────────────────────
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("recommendationId", recommendationId);
        responseData.put("recommendation",   recommendation);
        responseData.put("generatedAt",      doc.get("generatedAt"));

        return ResponseEntity.ok(new ResponseDto(200, "Recommendation generated successfully", responseData));
    }

    // ── GET RECOMMENDATION HISTORY ─────────────────────────────────────────────

    /**
     * Returns all past recommendations generated for the authenticated user.
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
     * Builds a structured health prompt from the user's recent metrics.
     * The prompt instructs the model to act as a health advisor and
     * provide specific, actionable recommendations.
     *
     * @param metrics list of recent health metric documents
     * @return formatted prompt string
     */
    private String buildHealthPrompt(List<Map<String, Object>> metrics) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are a professional health and fitness advisor. ");
        sb.append("Based on the following health metrics, provide 3 specific, ");
        sb.append("actionable, and personalized recommendations. ");
        sb.append("Be concise, encouraging, and practical. ");
        sb.append("Format your response as 3 numbered recommendations.\n\n");
        sb.append("Health metrics (most recent first):\n");

        for (int i = 0; i < metrics.size(); i++) {
            Map<String, Object> m = metrics.get(i);
            sb.append("\nEntry ").append(i + 1).append(":\n");

            // Append each metric if present — avoid null values in the prompt
            if (m.get("weightKg")   != null) sb.append("- Weight: ").append(m.get("weightKg")).append(" kg\n");
            if (m.get("heightCm")   != null) sb.append("- Height: ").append(m.get("heightCm")).append(" cm\n");
            if (m.get("bmi")        != null) sb.append("- BMI: ").append(m.get("bmi")).append("\n");
            if (m.get("heartRate")  != null) sb.append("- Heart rate: ").append(m.get("heartRate")).append(" bpm\n");
            if (m.get("systolic")   != null) sb.append("- Blood pressure: ")
                    .append(m.get("systolic")).append("/").append(m.get("diastolic")).append(" mmHg\n");
            if (m.get("notes")      != null) sb.append("- Notes: ").append(m.get("notes")).append("\n");
            if (m.get("recordedAt") != null) sb.append("- Recorded: ").append(m.get("recordedAt")).append("\n");
        }

        sb.append("\nProvide your 3 recommendations now:");
        return sb.toString();
    }

    // ── PRIVATE: CALL GROQ API ─────────────────────────────────────────────────

    /**
 * Sends the prompt to the Groq API and returns the model's response text.
 * The request body is serialized as a raw JSON string to avoid WebClient
 * type-erasure issues with nested Lists inside Maps.
 *
 * @param prompt the health prompt to send
 * @return the model's recommendation as a plain string
 * @throws RuntimeException if the API call fails
 */
    @SuppressWarnings("unchecked")
    private String callGroqApi(String prompt) {
        log.info("[RECOMMENDATION] Calling Groq API — model: {}", groqModel);

        // Escape the prompt for safe JSON embedding
        String safePrompt = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

        // Build request body as raw JSON string
        String requestBody = String.format(
            "{\"model\":\"%s\",\"temperature\":0.7,\"max_tokens\":500,\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
            groqModel, safePrompt
        );

        // Log the exact request body sent to Groq for debugging
        log.debug("[RECOMMENDATION] Request body: {}", requestBody);

        try {
            Map<String, Object> response = webClient.post()
                    .uri(groqApiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    // ── Log the exact Groq error body before throwing ──────────
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(errorBody -> {
                                    log.error("[RECOMMENDATION] Groq error response body: {}", errorBody);
                                    return new RuntimeException("Groq API error: " + errorBody);
                                })
                    )
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Groq API returned an empty response");
            }

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
