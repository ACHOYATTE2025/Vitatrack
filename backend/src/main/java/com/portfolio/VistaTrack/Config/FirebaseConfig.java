package com.portfolio.VistaTrack.Config;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FirebaseConfig {

     /**
     * Initializes Firebase and returns a ready-to-use Firestore instance.
     * This bean is injected into FirestoreService via @RequiredArgsConstructor.
     *
     * @return Firestore client connected to the VitaTrack project
     * @throws IOException if credentials cannot be loaded
     */
    @Bean
    public Firestore firestore() throws IOException {
        log.info("[FIREBASE] Initializing Firebase Admin SDK...");

        // ── 1. Load credentials ────────────────────────────────────────────
        GoogleCredentials credentials;
        String envCredentials = System.getenv("FIREBASE_CREDENTIALS");

        if (envCredentials != null && !envCredentials.isBlank()) {
            // Production (Railway) — credentials injected as environment variable
            log.info("[FIREBASE] Loading credentials from FIREBASE_CREDENTIALS env variable");
            credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(envCredentials.getBytes(StandardCharsets.UTF_8))
            );
        } else {
            // Local development — load from resources/serviceAccountKey.json
            log.info("[FIREBASE] Loading credentials from serviceAccountKey.json");
            InputStream serviceAccount = getClass()
                    .getClassLoader()
                    .getResourceAsStream("serviceAccountKey.json");

            if (serviceAccount == null) {
                throw new FileNotFoundException(
                    "serviceAccountKey.json not found in src/main/resources/ — " +
                    "download it from Firebase Console → Project Settings → Service Accounts"
                );
            }
            credentials = GoogleCredentials.fromStream(serviceAccount);
        }

        // ── 2. Delete existing FirebaseApp if present (DevTools hot reload) ─
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("[FIREBASE] Existing FirebaseApp detected — deleting before reinitializing");
            FirebaseApp.getInstance().delete();
        }

        // ── 3. Initialize FirebaseApp ──────────────────────────────────────
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        FirebaseApp.initializeApp(options);
        log.info("[FIREBASE] Firebase Admin SDK initialized successfully");

        // ── 4. Return the Firestore bean ───────────────────────────────────
        Firestore firestore = FirestoreClient.getFirestore();
        log.info("[FIREBASE] Firestore client ready");
        return firestore;
    }
}
