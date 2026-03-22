package com.portfolio.VistaTrack;



import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.portfolio.VistaTrack.Services.FirestoreService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FirestoreService.
 *
 * All Firestore interactions are mocked — no real Firebase connection needed.
 *
 * Coverage:
 *   save()         — success, ExecutionException
 *   findById()     — found, not found, ExecutionException
 *   findByField()  — results found, empty result, ExecutionException
 *   findAll()      — results found, empty, ExecutionException
 *   delete()       — success, ExecutionException
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FirestoreServiceTest {

    // ── Firestore chain mocks ──────────────────────────────────────────────────
    @Mock private Firestore           firestore;
    @Mock private CollectionReference collection;
    @Mock private DocumentReference   docRef;
    @Mock private Query               query;

    // ── Future + Snapshot mocks ────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    @Mock private ApiFuture<WriteResult>    writeFuture;
    @SuppressWarnings("unchecked")
    @Mock private ApiFuture<DocumentSnapshot> docFuture;
    @Mock private DocumentSnapshot            docSnapshot;
    @SuppressWarnings("unchecked")
    @Mock private ApiFuture<QuerySnapshot>    queryFuture;
    @Mock private QuerySnapshot               querySnapshot;

    @InjectMocks private FirestoreService firestoreService;

    // ── Constants ──────────────────────────────────────────────────────────────
    private static final String COLLECTION = "users";
    private static final String DOC_ID     = "user-123";
    private static final String FIELD      = "email";
    private static final String VALUE      = "john@example.com";

    @BeforeEach
    void setUp() {
        // Common chain: firestore.collection(X)
        when(firestore.collection(COLLECTION)).thenReturn(collection);
        when(collection.document(DOC_ID)).thenReturn(docRef);
        when(collection.whereEqualTo(FIELD, VALUE)).thenReturn(query);
    }

    // ── Helper ─────────────────────────────────────────────────────────────────
    private Map<String, Object> buildDoc() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id",    DOC_ID);
        doc.put("email", VALUE);
        doc.put("role",  "USER");
        return doc;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // save()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("✅ saves document without throwing")
        void savesDocumentSuccessfully() throws Exception {
            // Arrange
            when(docRef.set(anyMap())).thenReturn(writeFuture);
            when(writeFuture.get()).thenReturn(mock(WriteResult.class));

            // Act & Assert — no exception thrown
            firestoreService.save(COLLECTION, DOC_ID, buildDoc());
            verify(docRef).set(anyMap());
        }

        @Test
        @DisplayName("✅ passes the correct data map to Firestore")
        void passesCorrectDataToFirestore() throws Exception {
            // Arrange
            when(docRef.set(anyMap())).thenReturn(writeFuture);
            when(writeFuture.get()).thenReturn(mock(WriteResult.class));
            Map<String, Object> data = buildDoc();

            // Act
            firestoreService.save(COLLECTION, DOC_ID, data);

            // Capture and verify
            @SuppressWarnings("unchecked")
            var captor = org.mockito.ArgumentCaptor
                .forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
            verify(docRef).set(captor.capture());
            assertThat(captor.getValue().get("email")).isEqualTo(VALUE);
            assertThat(captor.getValue().get("id")).isEqualTo(DOC_ID);
        }

        @Test
        @DisplayName("❌ wraps ExecutionException in RuntimeException")
        void wrapsExecutionException() throws Exception {
            when(docRef.set(anyMap())).thenReturn(writeFuture);
            when(writeFuture.get())
                .thenThrow(new ExecutionException("write failed", new Exception()));

            assertThatThrownBy(() -> firestoreService.save(COLLECTION, DOC_ID, buildDoc()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Firestore write failed");
        }

        @Test
        @DisplayName("❌ wraps InterruptedException in RuntimeException")
        void wrapsInterruptedException() throws Exception {
            when(docRef.set(anyMap())).thenReturn(writeFuture);
            when(writeFuture.get()).thenThrow(new InterruptedException("interrupted"));

            assertThatThrownBy(() -> firestoreService.save(COLLECTION, DOC_ID, buildDoc()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Firestore write failed");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // findById()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findById()")
    class FindById {

        @BeforeEach
        void setUpDocGet() throws Exception {
            when(docRef.get()).thenReturn(docFuture);
        }

        @Test
        @DisplayName("✅ returns Optional with data when document exists")
        void returnsDataWhenDocumentExists() throws Exception {
            // Arrange
            when(docFuture.get()).thenReturn(docSnapshot);
            when(docSnapshot.exists()).thenReturn(true);
            when(docSnapshot.getData()).thenReturn(buildDoc());

            // Act
            Optional<Map<String, Object>> result =
                firestoreService.findById(COLLECTION, DOC_ID);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().get("email")).isEqualTo(VALUE);
            assertThat(result.get().get("id")).isEqualTo(DOC_ID);
        }

        @Test
        @DisplayName("✅ returns empty Optional when document does not exist")
        void returnsEmptyWhenDocumentNotFound() throws Exception {
            when(docFuture.get()).thenReturn(docSnapshot);
            when(docSnapshot.exists()).thenReturn(false);

            Optional<Map<String, Object>> result =
                firestoreService.findById(COLLECTION, DOC_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("❌ wraps ExecutionException in RuntimeException")
        void wrapsExecutionException() throws Exception {
            when(docFuture.get())
                .thenThrow(new ExecutionException("read failed", new Exception()));

            assertThatThrownBy(() -> firestoreService.findById(COLLECTION, DOC_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Firestore read failed");
        }

        @Test
        @DisplayName("❌ wraps InterruptedException in RuntimeException")
        void wrapsInterruptedException() throws Exception {
            when(docFuture.get()).thenThrow(new InterruptedException("interrupted"));

            assertThatThrownBy(() -> firestoreService.findById(COLLECTION, DOC_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Firestore read failed");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // findByField()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByField()")
    class FindByField {

        @BeforeEach
        void setUpQuery() throws Exception {
            when(query.get()).thenReturn(queryFuture);
            when(queryFuture.get()).thenReturn(querySnapshot);
        }

        @Test
        @DisplayName("✅ returns list of matching documents")
        void returnsMatchingDocuments() throws Exception {
            // Arrange — two matching documents
            QueryDocumentSnapshot snap1 = mock(QueryDocumentSnapshot.class);
            QueryDocumentSnapshot snap2 = mock(QueryDocumentSnapshot.class);
            when(snap1.getData()).thenReturn(Map.of("id", "user-1", "email", VALUE));
            when(snap2.getData()).thenReturn(Map.of("id", "user-2", "email", VALUE));
            when(querySnapshot.getDocuments()).thenReturn(List.of(snap1, snap2));

            // Act
            List<Map<String, Object>> results =
                firestoreService.findByField(COLLECTION, FIELD, VALUE);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results.get(0).get("id")).isEqualTo("user-1");
            assertThat(results.get(1).get("id")).isEqualTo("user-2");
        }

        @Test
        @DisplayName("✅ returns empty list when no documents match")
        void returnsEmptyListWhenNoMatch() throws Exception {
            when(querySnapshot.getDocuments()).thenReturn(List.of());

            List<Map<String, Object>> results =
                firestoreService.findByField(COLLECTION, FIELD, VALUE);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("✅ returns exactly one document when only one matches")
        void returnsOneDocument() throws Exception {
            QueryDocumentSnapshot snap = mock(QueryDocumentSnapshot.class);
            when(snap.getData()).thenReturn(buildDoc());
            when(querySnapshot.getDocuments()).thenReturn(List.of(snap));

            List<Map<String, Object>> results =
                firestoreService.findByField(COLLECTION, FIELD, VALUE);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).get("email")).isEqualTo(VALUE);
        }

        @Test
        @DisplayName("❌ wraps ExecutionException in RuntimeException")
        void wrapsExecutionException() throws Exception {
            when(queryFuture.get())
                .thenThrow(new ExecutionException("query failed", new Exception()));

            assertThatThrownBy(() ->
                firestoreService.findByField(COLLECTION, FIELD, VALUE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Firestore query failed");
        }

        @Test
        @DisplayName("❌ wraps InterruptedException in RuntimeException")
        void wrapsInterruptedException() throws Exception {
            when(queryFuture.get()).thenThrow(new InterruptedException("interrupted"));

            assertThatThrownBy(() ->
                firestoreService.findByField(COLLECTION, FIELD, VALUE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Firestore query failed");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // findAll()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @SuppressWarnings("unchecked")
        @Mock private ApiFuture<QuerySnapshot> allFuture;
        @Mock private QuerySnapshot            allSnapshot;

        @BeforeEach
        void setUpFindAll() throws Exception {
            when(collection.get()).thenReturn(allFuture);
            when(allFuture.get()).thenReturn(allSnapshot);
        }

        @Test
        @DisplayName("✅ returns all documents in collection")
        void returnsAllDocuments() throws Exception {
            QueryDocumentSnapshot d1 = mock(QueryDocumentSnapshot.class);
            QueryDocumentSnapshot d2 = mock(QueryDocumentSnapshot.class);
            QueryDocumentSnapshot d3 = mock(QueryDocumentSnapshot.class);
            when(d1.getData()).thenReturn(Map.of("id", "u1"));
            when(d2.getData()).thenReturn(Map.of("id", "u2"));
            when(d3.getData()).thenReturn(Map.of("id", "u3"));
            when(allSnapshot.getDocuments()).thenReturn(List.of(d1, d2, d3));

            List<Map<String, Object>> results = firestoreService.findAll(COLLECTION);

            assertThat(results).hasSize(3);
            assertThat(results.get(0).get("id")).isEqualTo("u1");
            assertThat(results.get(2).get("id")).isEqualTo("u3");
        }

        @Test
        @DisplayName("✅ returns empty list when collection is empty")
        void returnsEmptyListWhenCollectionEmpty() throws Exception {
            when(allSnapshot.getDocuments()).thenReturn(List.of());

            List<Map<String, Object>> results = firestoreService.findAll(COLLECTION);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("❌ wraps ExecutionException in RuntimeException")
        void wrapsExecutionException() throws Exception {
            when(allFuture.get())
                .thenThrow(new ExecutionException("read-all failed", new Exception()));

            assertThatThrownBy(() -> firestoreService.findAll(COLLECTION))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Firestore read-all failed");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // delete()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("✅ deletes document without throwing")
        void deletesDocumentSuccessfully() throws Exception {
            when(docRef.delete()).thenReturn(writeFuture);
            when(writeFuture.get()).thenReturn(mock(WriteResult.class));

            firestoreService.delete(COLLECTION, DOC_ID);

            verify(docRef).delete();
        }

        @Test
        @DisplayName("✅ calls delete on the correct document reference")
        void callsDeleteOnCorrectDocument() throws Exception {
            when(docRef.delete()).thenReturn(writeFuture);
            when(writeFuture.get()).thenReturn(mock(WriteResult.class));

            firestoreService.delete(COLLECTION, DOC_ID);

            // Verify the correct collection and document were targeted
            verify(firestore).collection(COLLECTION);
            verify(collection).document(DOC_ID);
            verify(docRef).delete();
        }

        @Test
        @DisplayName("❌ wraps ExecutionException in RuntimeException")
        void wrapsExecutionException() throws Exception {
            when(docRef.delete()).thenReturn(writeFuture);
            when(writeFuture.get())
                .thenThrow(new ExecutionException("delete failed", new Exception()));

            assertThatThrownBy(() -> firestoreService.delete(COLLECTION, DOC_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Firestore delete failed");
        }

        @Test
        @DisplayName("❌ wraps InterruptedException in RuntimeException")
        void wrapsInterruptedException() throws Exception {
            when(docRef.delete()).thenReturn(writeFuture);
            when(writeFuture.get()).thenThrow(new InterruptedException("interrupted"));

            assertThatThrownBy(() -> firestoreService.delete(COLLECTION, DOC_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Firestore delete failed");
        }
    }
}