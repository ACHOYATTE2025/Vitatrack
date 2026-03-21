package com.portfolio.VistaTrack.Services;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic CRUD wrapper around Firestore.
 * All other services use this class to read and write data —
 * they never call Firestore directly.
 *
 * Firestore structure:
 *   users/          → one document per user
 *   activities/     → one document per activity log
 *   health_metrics/ → one document per health entry
 *   goals/          → one document per goal
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirestoreService {

      private final Firestore firestore;

    // ── WRITE ──────────────────────────────────────────────────────────────────

    /**
     * Creates or fully overwrites a document at collection/documentId.
     * If the document does not exist it is created; if it exists it is replaced.
     *
     * @param collection Firestore collection name (e.g. "users")
     * @param documentId unique document ID (e.g. user UUID)
     * @param data       map of field → value to persist
     */
    public void save(String collection, String documentId, Map<String, Object> data) {
        try {
            log.info("[FIRESTORE] Saving document — collection: {} | id: {}", collection, documentId);
            ApiFuture<WriteResult> future = firestore.collection(collection)
                    .document(documentId)
                    .set(data);
            future.get(); // block until Firestore confirms the write
            log.info("[FIRESTORE] Document saved successfully — collection: {} | id: {}", collection, documentId);
        } catch (InterruptedException | ExecutionException e) {
            log.error("[FIRESTORE] Failed to save document — collection: {} | id: {} | error: {}",
                    collection, documentId, e.getMessage());
            throw new RuntimeException("Firestore write failed: " + e.getMessage());
        }
    }

    // ── READ BY ID ─────────────────────────────────────────────────────────────

    /**
     * Retrieves a single document by its ID.
     * Returns an empty Optional if the document does not exist.
     *
     * @param collection Firestore collection name
     * @param documentId document ID to fetch
     * @return Optional containing the document data as a Map, or empty if not found
     */
    public Optional<Map<String, Object>> findById(String collection, String documentId) {
        try {
            log.info("[FIRESTORE] Fetching document — collection: {} | id: {}", collection, documentId);
            DocumentSnapshot snapshot = firestore.collection(collection)
                    .document(documentId)
                    .get()
                    .get();

            if (!snapshot.exists()) {
                log.warn("[FIRESTORE] Document not found — collection: {} | id: {}", collection, documentId);
                return Optional.empty();
            }

            log.info("[FIRESTORE] Document found — collection: {} | id: {}", collection, documentId);
            return Optional.ofNullable(snapshot.getData());

        } catch (InterruptedException | ExecutionException e) {
            log.error("[FIRESTORE] Failed to fetch document — collection: {} | id: {} | error: {}",
                    collection, documentId, e.getMessage());
            throw new RuntimeException("Firestore read failed: " + e.getMessage());
        }
    }


    // ── QUERY BY FIELD ─────────────────────────────────────────────────────────

    /**
     * Finds all documents in a collection where a given field equals a given value.
     * Example: findByField("users", "email", "john@example.com")
     *
     * @param collection  Firestore collection name
     * @param field       field name to filter on
     * @param value       expected value
     * @return list of matching documents as Maps (empty list if none found)
     */
    public List<Map<String, Object>> findByField(String collection, String field, Object value) {
        try {
            log.info("[FIRESTORE] Querying — collection: {} | {}={}", collection, field, value);
            ApiFuture<QuerySnapshot> future = firestore.collection(collection)
                    .whereEqualTo(field, value)
                    .get();

            List<Map<String, Object>> results = future.get().getDocuments()
                    .stream()
                    .map(DocumentSnapshot::getData)
                    .collect(Collectors.toList());

            log.info("[FIRESTORE] Query returned {} result(s) — collection: {} | {}={}",
                    results.size(), collection, field, value);
            return results;

        } catch (InterruptedException | ExecutionException e) {
            log.error("[FIRESTORE] Query failed — collection: {} | error: {}", collection, e.getMessage());
            throw new RuntimeException("Firestore query failed: " + e.getMessage());
        }
    }


    // ── READ ALL ───────────────────────────────────────────────────────────────

    /**
     * Returns all documents in a collection.
     * Use with care on large collections — no pagination here.
     *
     * @param collection Firestore collection name
     * @return list of all documents as Maps
     */
    public List<Map<String, Object>> findAll(String collection) {
        try {
            log.info("[FIRESTORE] Fetching all documents — collection: {}", collection);
            List<Map<String, Object>> results = firestore.collection(collection)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(DocumentSnapshot::getData)
                    .collect(Collectors.toList());

            log.info("[FIRESTORE] Found {} document(s) — collection: {}", results.size(), collection);
            return results;

        } catch (InterruptedException | ExecutionException e) {
            log.error("[FIRESTORE] Failed to fetch all — collection: {} | error: {}", collection, e.getMessage());
            throw new RuntimeException("Firestore read-all failed: " + e.getMessage());
        }
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────

    /**
     * Deletes a document by its ID.
     * No error is thrown if the document does not exist.
     *
     * @param collection Firestore collection name
     * @param documentId document ID to delete
     */
    public void delete(String collection, String documentId) {
        try {
            log.info("[FIRESTORE] Deleting document — collection: {} | id: {}", collection, documentId);
            firestore.collection(collection).document(documentId).delete().get();
            log.info("[FIRESTORE] Document deleted — collection: {} | id: {}", collection, documentId);
        } catch (InterruptedException | ExecutionException e) {
            log.error("[FIRESTORE] Delete failed — collection: {} | id: {} | error: {}",
                    collection, documentId, e.getMessage());
            throw new RuntimeException("Firestore delete failed: " + e.getMessage());
        }
    }

}
