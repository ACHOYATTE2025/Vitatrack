package com.portfolio.VistaTrack.Config;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import lombok.extern.slf4j.Slf4j;


/**
 * Initializes the Firebase Admin SDK at application startup.
 * Exposes a Firestore bean used by FirestoreService for all database operations.
 *
 * Handles Spring DevTools hot reload safely — if a previous FirebaseApp instance
 * exists and its Firestore client is closed, it is deleted before reinitializing.
 */
@Slf4j
@Configuration
public class WebClientConfig {

       /**
     * Provides a default WebClient instance.
     * RecommendationService injects this bean and sets the full URL per request.
     *
     * @return a configured WebClient bean
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}