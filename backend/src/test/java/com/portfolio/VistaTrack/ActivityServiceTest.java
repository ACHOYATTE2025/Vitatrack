package com.portfolio.VistaTrack;


import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.portfolio.VistaTrack.Dto.ActivityRequestDto;
import com.portfolio.VistaTrack.Dto.ResponseDto;
import com.portfolio.VistaTrack.Exception.ResourceNotFoundException;
import com.portfolio.VistaTrack.Services.ActivityService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;


// Par celle-ci
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ActivityServiceTest {

    // ── Firestore chain mocks ──────────────────────────────────────────────────
    @Mock private Firestore                               firestore;
    @Mock private CollectionReference                     metricsCollection;
    @Mock private DocumentReference                       metricDocRef;
    @Mock private CollectionReference                     activitiesSubcollection;
    @Mock private DocumentReference                       activityDocRef;

    // ── Future + Snapshot mocks ────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    @Mock private ApiFuture<DocumentSnapshot>             metricFuture;
    @Mock private DocumentSnapshot                        metricSnapshot;

    @SuppressWarnings("unchecked")
    @Mock private ApiFuture<DocumentSnapshot>             activityFuture;
    @Mock private DocumentSnapshot                        activitySnapshot;

    @SuppressWarnings("unchecked")
    @Mock private ApiFuture<WriteResult>                  writeFuture;

    @SuppressWarnings("unchecked")
    @Mock private ApiFuture<QuerySnapshot>                queryFuture;
    @Mock private QuerySnapshot                           querySnapshot;

    @InjectMocks private ActivityService activityService;

    // ── Constants ──────────────────────────────────────────────────────────────
    private static final String METRIC_ID   = "metric-123";
    private static final String ACTIVITY_ID = "activity-456";
    private static final String USER_ID     = "user-789";
    private static final String OTHER_USER  = "other-999";

    private ActivityRequestDto validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ActivityRequestDto("Running", 45, 380.5, "Morning run");

        // Firestore chain: firestore.collection("health_metrics").document(metricId)
        when(firestore.collection("health_metrics")).thenReturn(metricsCollection);
        when(metricsCollection.document(METRIC_ID)).thenReturn(metricDocRef);
        when(metricDocRef.collection("activities")).thenReturn(activitiesSubcollection);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // logActivity()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("logActivity()")
    class LogActivity {

        @Test
        @DisplayName("✅ returns HTTP 201 when activity logged successfully")
        void returns201WhenSuccess() throws Exception {
            // Arrange
            stubMetricExists(USER_ID);
            when(activitiesSubcollection.document(anyString())).thenReturn(activityDocRef);
            when(activityDocRef.set(anyMap())).thenReturn(writeFuture);
            when(writeFuture.get()).thenReturn(mock(WriteResult.class));

            // Act
            ResponseEntity<ResponseDto> response =
                activityService.logActivity(validRequest, METRIC_ID, USER_ID);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatusCode()).isEqualTo(201);
            assertThat(response.getBody().getStatusMsg()).contains("successfully");
            verify(activityDocRef).set(anyMap());
        }

        @Test
        @DisplayName("✅ saves all fields correctly in Firestore document")
        void savesAllFields() throws Exception {
            // Arrange
            stubMetricExists(USER_ID);
            when(activitiesSubcollection.document(anyString())).thenReturn(activityDocRef);
            when(activityDocRef.set(anyMap())).thenReturn(writeFuture);
            when(writeFuture.get()).thenReturn(mock(WriteResult.class));

            // Act
            activityService.logActivity(validRequest, METRIC_ID, USER_ID);

            // Capture what was saved
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor =
                ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
            verify(activityDocRef).set(captor.capture());

            Map<String, Object> saved = captor.getValue();
            assertThat(saved.get("userId")).isEqualTo(USER_ID);
            assertThat(saved.get("metricId")).isEqualTo(METRIC_ID);
            assertThat(saved.get("type")).isEqualTo("Running");
            assertThat(saved.get("durationMinutes")).isEqualTo(45);
            assertThat(saved.get("caloriesBurned")).isEqualTo(380.5);
            assertThat(saved.get("notes")).isEqualTo("Morning run");
            assertThat(saved.get("id")).isNotNull();
            assertThat(saved.get("loggedAt")).isNotNull();
        }

        @Test
        @DisplayName("❌ throws ResourceNotFoundException when metric not found")
        void throwsWhenMetricNotFound() throws Exception {
            stubMetricNotFound();

            assertThatThrownBy(() ->
                activityService.logActivity(validRequest, METRIC_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(METRIC_ID);

            verify(activityDocRef, never()).set(anyMap());
        }

        @Test
        @DisplayName("❌ throws RuntimeException when metric belongs to another user")
        void throwsWhenWrongOwner() throws Exception {
            stubMetricExists(OTHER_USER);

            assertThatThrownBy(() ->
                activityService.logActivity(validRequest, METRIC_ID, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");

            verify(activityDocRef, never()).set(anyMap());
        }

        @Test
        @DisplayName("❌ wraps InterruptedException in RuntimeException")
        void wrapsInterruptedException() throws Exception {
            when(metricDocRef.get()).thenReturn(metricFuture);
            when(metricFuture.get()).thenThrow(new InterruptedException("timeout"));

            assertThatThrownBy(() ->
                activityService.logActivity(validRequest, METRIC_ID, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to log activity");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getActivitiesForMetric()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getActivitiesForMetric()")
    class GetActivitiesForMetric {

        @Test
        @DisplayName("✅ returns 200 with activity list")
        void returns200WithActivities() throws Exception {
            stubMetricExists(USER_ID);

            when(activitiesSubcollection.get()).thenReturn(queryFuture);
            when(queryFuture.get()).thenReturn(querySnapshot);

            QueryDocumentSnapshot doc1 = mock(QueryDocumentSnapshot.class);
            QueryDocumentSnapshot doc2 = mock(QueryDocumentSnapshot.class);
            when(doc1.getData()).thenReturn(buildActivity("act-1", "Running"));
            when(doc2.getData()).thenReturn(buildActivity("act-2", "Cycling"));
            when(querySnapshot.getDocuments()).thenReturn(List.of(doc1, doc2));

            ResponseEntity<List<Map<String, Object>>> response =
                activityService.getActivitiesForMetric(METRIC_ID, USER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).get("type")).isEqualTo("Running");
            assertThat(response.getBody().get(1).get("type")).isEqualTo("Cycling");
        }

        @Test
        @DisplayName("✅ returns empty list when no activities")
        void returnsEmptyList() throws Exception {
            stubMetricExists(USER_ID);

            when(activitiesSubcollection.get()).thenReturn(queryFuture);
            when(queryFuture.get()).thenReturn(querySnapshot);
            when(querySnapshot.getDocuments()).thenReturn(List.of());

            ResponseEntity<List<Map<String, Object>>> response =
                activityService.getActivitiesForMetric(METRIC_ID, USER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }

        @Test
        @DisplayName("❌ throws ResourceNotFoundException when metric not found")
        void throwsWhenMetricNotFound() throws Exception {
            stubMetricNotFound();

            assertThatThrownBy(() ->
                activityService.getActivitiesForMetric(METRIC_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("❌ throws RuntimeException when user does not own the metric")
        void throwsWhenWrongOwner() throws Exception {
            stubMetricExists(OTHER_USER);

            assertThatThrownBy(() ->
                activityService.getActivitiesForMetric(METRIC_ID, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // deleteActivity()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteActivity()")
    class DeleteActivity {

        @BeforeEach
        void setUpActivity() {
            when(activitiesSubcollection.document(ACTIVITY_ID)).thenReturn(activityDocRef);
            when(activityDocRef.get()).thenReturn(activityFuture);
        }

        @Test
        @DisplayName("✅ returns 200 when activity deleted successfully")
        void returns200WhenDeleted() throws Exception {
            stubActivityExists(USER_ID);
            when(activityDocRef.delete()).thenReturn(writeFuture);
            when(writeFuture.get()).thenReturn(mock(WriteResult.class));

            ResponseEntity<ResponseDto> response =
                activityService.deleteActivity(METRIC_ID, ACTIVITY_ID, USER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatusCode()).isEqualTo(200);
            assertThat(response.getBody().getStatusMsg()).contains("successfully");
            verify(activityDocRef).delete();
        }

        @Test
        @DisplayName("❌ throws ResourceNotFoundException when activity not found")
        void throwsWhenActivityNotFound() throws Exception {
            when(activityFuture.get()).thenReturn(activitySnapshot);
            when(activitySnapshot.exists()).thenReturn(false);

            assertThatThrownBy(() ->
                activityService.deleteActivity(METRIC_ID, ACTIVITY_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(ACTIVITY_ID);

            verify(activityDocRef, never()).delete();
        }

        @Test
        @DisplayName("❌ throws RuntimeException when activity belongs to another user")
        void throwsWhenWrongOwner() throws Exception {
            stubActivityExists(OTHER_USER);

            assertThatThrownBy(() ->
                activityService.deleteActivity(METRIC_ID, ACTIVITY_ID, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");

            verify(activityDocRef, never()).delete();
        }

        @Test
        @DisplayName("❌ wraps ExecutionException in RuntimeException")
        void wrapsExecutionException() throws Exception {
            when(activityFuture.get())
                .thenThrow(new ExecutionException("Firestore error", new Exception()));

            assertThatThrownBy(() ->
                activityService.deleteActivity(METRIC_ID, ACTIVITY_ID, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to delete activity");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helper methods — reduce boilerplate in tests
    // ══════════════════════════════════════════════════════════════════════════

    /** Stubs the metric document to exist and belong to the given userId */
    private void stubMetricExists(String ownerId) throws Exception {
        when(metricDocRef.get()).thenReturn(metricFuture);
        when(metricFuture.get()).thenReturn(metricSnapshot);
        when(metricSnapshot.exists()).thenReturn(true);
        when(metricSnapshot.getData()).thenReturn(Map.of("userId", ownerId));
    }

    /** Stubs the metric document to not exist */
    private void stubMetricNotFound() throws Exception {
        when(metricDocRef.get()).thenReturn(metricFuture);
        when(metricFuture.get()).thenReturn(metricSnapshot);
        when(metricSnapshot.exists()).thenReturn(false);
    }

    /** Stubs the activity document to exist and belong to the given userId */
    private void stubActivityExists(String ownerId) throws Exception {
        when(activityFuture.get()).thenReturn(activitySnapshot);
        when(activitySnapshot.exists()).thenReturn(true);
        when(activitySnapshot.getData()).thenReturn(Map.of("userId", ownerId));
    }

    /** Builds a minimal activity map for use in query result stubs */
    private Map<String, Object> buildActivity(String id, String type) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",   id);
        m.put("type", type);
        return m;
    }
}