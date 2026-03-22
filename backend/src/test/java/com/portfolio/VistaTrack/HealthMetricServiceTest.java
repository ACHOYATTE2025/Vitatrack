package com.portfolio.VistaTrack;



import com.portfolio.VistaTrack.Dto.HealthMetricRequestDto;
import com.portfolio.VistaTrack.Dto.ResponseDto;
import com.portfolio.VistaTrack.Exception.ResourceNotFoundException;
import com.portfolio.VistaTrack.Services.FirestoreService;
import com.portfolio.VistaTrack.Services.HealthMetricService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HealthMetricService.
 *
 * FirestoreService is mocked — no real Firestore connection needed.
 *
 * Coverage:
 *   recordMetric()   — success, BMI computed, BMI null when data missing
 *   getUserMetrics() — list returned, empty list
 *   getMetricById()  — found, not found, wrong owner
 *   deleteMetric()   — success, not found, wrong owner
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HealthMetricServiceTest {

    @Mock private FirestoreService firestoreService;

    @InjectMocks private HealthMetricService healthMetricService;

    // ── Constants ──────────────────────────────────────────────────────────────
    private static final String COLLECTION = "health_metrics";
    private static final String METRIC_ID  = "metric-abc";
    private static final String USER_ID    = "user-xyz";
    private static final String OTHER_USER = "other-000";

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Builds a full HealthMetricRequestDto with weight + height for BMI computation */
    private HealthMetricRequestDto fullRequest() {
        return new HealthMetricRequestDto(75.0, 178.0, 68, 120, 80, "Feeling good");
    }

    /** Builds a request without weight/height — BMI should be null */
    private HealthMetricRequestDto partialRequest() {
        return new HealthMetricRequestDto(null, null, 72, 115, 78, "Just heart rate");
    }

    /** Builds a stored metric document belonging to USER_ID */
    private Map<String, Object> buildMetricDoc() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id",         METRIC_ID);
        doc.put("userId",     USER_ID);
        doc.put("weightKg",   75.0);
        doc.put("heightCm",   178.0);
        doc.put("bmi",        23.67);
        doc.put("heartRate",  68);
        doc.put("systolic",   120);
        doc.put("diastolic",  80);
        doc.put("notes",      "Feeling good");
        doc.put("recordedAt", "2026-03-22T10:00:00Z");
        return doc;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // recordMetric()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("recordMetric()")
    class RecordMetric {

        @Test
        @DisplayName("✅ returns HTTP 201 on success")
        void returns201OnSuccess() {
            // Act
            ResponseEntity<ResponseDto> response =
                healthMetricService.recordMetric(fullRequest(), USER_ID);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatusCode()).isEqualTo(201);
            assertThat(response.getBody().getStatusMsg()).contains("successfully");
        }

        @Test
        @DisplayName("✅ returns the generated metricId in the response data")
        void returnsMetricIdInData() {
            ResponseEntity<ResponseDto> response =
                healthMetricService.recordMetric(fullRequest(), USER_ID);

            assertThat(response.getBody().getData()).isNotNull();
            assertThat(response.getBody().getData().toString()).isNotBlank();
        }

        @Test
        @DisplayName("✅ computes BMI correctly when weight and height are provided")
        void computesBmiCorrectly() {
            // weight=75kg, height=178cm → BMI = 75 / (1.78²) = 23.67
            healthMetricService.recordMetric(fullRequest(), USER_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor =
                ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
            verify(firestoreService).save(eq(COLLECTION), anyString(), captor.capture());

            Object bmi = captor.getValue().get("bmi");
            assertThat(bmi).isNotNull();
            assertThat((Double) bmi).isBetween(23.0, 24.0);
        }

        @Test
        @DisplayName("✅ BMI is null when weight or height is missing")
        void bmiIsNullWhenDataMissing() {
            healthMetricService.recordMetric(partialRequest(), USER_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor =
                ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
            verify(firestoreService).save(eq(COLLECTION), anyString(), captor.capture());

            assertThat(captor.getValue().get("bmi")).isNull();
        }

        @Test
        @DisplayName("✅ saves all fields including userId and recordedAt")
        void savesAllFields() {
            healthMetricService.recordMetric(fullRequest(), USER_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor =
                ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
            verify(firestoreService).save(eq(COLLECTION), anyString(), captor.capture());

            Map<String, Object> saved = captor.getValue();
            assertThat(saved.get("userId")).isEqualTo(USER_ID);
            assertThat(saved.get("weightKg")).isEqualTo(75.0);
            assertThat(saved.get("heightCm")).isEqualTo(178.0);
            assertThat(saved.get("heartRate")).isEqualTo(68);
            assertThat(saved.get("systolic")).isEqualTo(120);
            assertThat(saved.get("diastolic")).isEqualTo(80);
            assertThat(saved.get("notes")).isEqualTo("Feeling good");
            assertThat(saved.get("id")).isNotNull();
            assertThat(saved.get("recordedAt")).isNotNull();
        }

        @Test
        @DisplayName("✅ saves exactly once per call")
        void savesExactlyOnce() {
            healthMetricService.recordMetric(fullRequest(), USER_ID);
            verify(firestoreService, times(1)).save(eq(COLLECTION), anyString(), anyMap());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getUserMetrics()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getUserMetrics()")
    class GetUserMetrics {

        @Test
        @DisplayName("✅ returns HTTP 200 with list of metrics")
        void returns200WithMetrics() {
            when(firestoreService.findByField(COLLECTION, "userId", USER_ID))
                .thenReturn(List.of(buildMetricDoc(), buildMetricDoc()));

            ResponseEntity<List<Map<String, Object>>> response =
                healthMetricService.getUserMetrics(USER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("✅ returns empty list when user has no metrics")
        void returnsEmptyList() {
            when(firestoreService.findByField(COLLECTION, "userId", USER_ID))
                .thenReturn(List.of());

            ResponseEntity<List<Map<String, Object>>> response =
                healthMetricService.getUserMetrics(USER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }

        @Test
        @DisplayName("✅ queries Firestore with correct userId filter")
        void queriesWithCorrectUserId() {
            when(firestoreService.findByField(COLLECTION, "userId", USER_ID))
                .thenReturn(List.of());

            healthMetricService.getUserMetrics(USER_ID);

            verify(firestoreService).findByField(COLLECTION, "userId", USER_ID);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getMetricById()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getMetricById()")
    class GetMetricById {

        @Test
        @DisplayName("✅ returns HTTP 200 with metric when found and owned by user")
        void returns200WhenFoundAndOwned() {
            when(firestoreService.findById(COLLECTION, METRIC_ID))
                .thenReturn(Optional.of(buildMetricDoc()));

            ResponseEntity<Map<String, Object>> response =
                healthMetricService.getMetricById(METRIC_ID, USER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("id")).isEqualTo(METRIC_ID);
            assertThat(response.getBody().get("userId")).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("❌ throws ResourceNotFoundException when metric does not exist")
        void throwsWhenMetricNotFound() {
            when(firestoreService.findById(COLLECTION, METRIC_ID))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                healthMetricService.getMetricById(METRIC_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(METRIC_ID);
        }

        @Test
        @DisplayName("❌ throws RuntimeException when metric belongs to another user")
        void throwsWhenWrongOwner() {
            when(firestoreService.findById(COLLECTION, METRIC_ID))
                .thenReturn(Optional.of(buildMetricDoc()));

            assertThatThrownBy(() ->
                healthMetricService.getMetricById(METRIC_ID, OTHER_USER))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // deleteMetric()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteMetric()")
    class DeleteMetric {

        @Test
        @DisplayName("✅ returns HTTP 200 when metric deleted successfully")
        void returns200WhenDeleted() {
            when(firestoreService.findById(COLLECTION, METRIC_ID))
                .thenReturn(Optional.of(buildMetricDoc()));

            ResponseEntity<ResponseDto> response =
                healthMetricService.deleteMetric(METRIC_ID, USER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatusCode()).isEqualTo(200);
            assertThat(response.getBody().getStatusMsg()).contains("successfully");
        }

        @Test
        @DisplayName("✅ calls Firestore delete with correct metricId")
        void callsDeleteWithCorrectId() {
            when(firestoreService.findById(COLLECTION, METRIC_ID))
                .thenReturn(Optional.of(buildMetricDoc()));

            healthMetricService.deleteMetric(METRIC_ID, USER_ID);

            verify(firestoreService).delete(COLLECTION, METRIC_ID);
        }

        @Test
        @DisplayName("❌ throws ResourceNotFoundException when metric does not exist")
        void throwsWhenMetricNotFound() {
            when(firestoreService.findById(COLLECTION, METRIC_ID))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                healthMetricService.deleteMetric(METRIC_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(METRIC_ID);

            verify(firestoreService, never()).delete(any(), any());
        }

        @Test
        @DisplayName("❌ throws RuntimeException when metric belongs to another user")
        void throwsWhenWrongOwner() {
            when(firestoreService.findById(COLLECTION, METRIC_ID))
                .thenReturn(Optional.of(buildMetricDoc()));

            assertThatThrownBy(() ->
                healthMetricService.deleteMetric(METRIC_ID, OTHER_USER))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");

            verify(firestoreService, never()).delete(any(), any());
        }

        @Test
        @DisplayName("❌ never deletes when ownership check fails")
        void neverDeletesWhenOwnershipFails() {
            when(firestoreService.findById(COLLECTION, METRIC_ID))
                .thenReturn(Optional.of(buildMetricDoc()));

            try {
                healthMetricService.deleteMetric(METRIC_ID, OTHER_USER);
            } catch (RuntimeException ignored) {}

            verify(firestoreService, never()).delete(any(), any());
        }
    }
}