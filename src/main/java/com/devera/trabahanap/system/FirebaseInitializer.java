package com.devera.trabahanap.system;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * FirebaseInitializer: single place to initialize Firebase Admin SDK using config.properties values.
 */
public final class FirebaseInitializer {

    private static volatile boolean initialized = false;

        private FirebaseInitializer() {}

    public static synchronized void ensureInitialized() {
        if (initialized) return;

        String serviceAccountPath = Config.get("firebase.serviceAccountPath");
        String databaseUrl = Config.get("firebase.databaseUrl");

        if (serviceAccountPath == null || databaseUrl == null) {
            System.err.println("Firebase configuration missing (firebase.serviceAccountPath or firebase.databaseUrl). Skipping initialization.");
            return;
        }

        try (FileInputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(databaseUrl)
                    .build();
            FirebaseApp.initializeApp(options);
            initialized = true;
            System.out.println("Firebase initialized.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }
}
