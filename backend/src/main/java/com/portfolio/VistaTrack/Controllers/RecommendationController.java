package com.portfolio.VistaTrack.Controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.VistaTrack.Dto.ResponseDto;
import com.portfolio.VistaTrack.Services.RecommendationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/recommendations")
@Tag(
    name = "Recommendations",
    description = "AI-powered health recommendations generated from the user's health metrics " +
                  "using the Groq API (llama3 model). Each call generates and saves a new recommendation."
)
public class RecommendationController {
     private final RecommendationService recommendationService;

    /**
     * Generates a personalized health recommendation based on the user's
     * most recent health metrics stored in Firestore.
     * Requires at least one health metric entry to exist.
     *
     * @param userId the authenticated user's ID — injected from the JWT
     * @return HTTP 200 with the recommendation text and its Firestore ID
     */
    @Operation(
        summary = "Generate a health recommendation",
        description = "Analyzes the user's 3 most recent health metrics and generates " +
                      "3 personalized, actionable recommendations using the Groq llama3 model. " +
                      "The recommendation is saved in Firestore for future reference.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recommendation generated and saved successfully"),
        @ApiResponse(responseCode = "400", description = "No health metrics found for this user"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "500", description = "Groq API call failed")
    })
    @PostMapping
    public ResponseEntity<ResponseDto> generateRecommendation(
            @AuthenticationPrincipal String userId) {

        log.info("[RECOMMENDATION CONTROLLER] POST /recommendations — userId: {}", userId);
        return recommendationService.generateRecommendation(userId);
    }

    /**
     * Returns the full recommendation history for the authenticated user,
     * sorted by most recent first.
     *
     * @param userId the authenticated user's ID — injected from the JWT
     * @return HTTP 200 with a list of past recommendation documents
     */
    @Operation(
        summary = "Get recommendation history",
        description = "Returns all past AI-generated recommendations for the current user, " +
                      "sorted by most recent first.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recommendation history returned successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getRecommendations(
            @AuthenticationPrincipal String userId) {

        log.info("[RECOMMENDATION CONTROLLER] GET /recommendations — userId: {}", userId);
        return recommendationService.getRecommendations(userId);
    }

}
