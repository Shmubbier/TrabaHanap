package com.devera.trabahanap.system;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

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

    /**
     * Return an OAuth2 access token string usable for Firestore REST calls.
     * This method constructs GoogleCredentials from the configured service account file
     * (firebase.serviceAccountPath) and requests appropriate scopes. If initialization
     * has already loaded credentials into FirebaseApp and you prefer to reuse those,
     * adapt this method accordingly to return the token from the stored credentials.
     *
     * @return OAuth2 token value (not "Bearer " prefix)
     * @throws IOException when credentials cannot be loaded or refreshed
     */
    public static String getAccessToken() throws IOException {
        String serviceAccountPath = Config.get("firebase.serviceAccountPath");
        if (serviceAccountPath == null) {
            throw new IOException("firebase.serviceAccountPath not configured");
        }

        // Scopes for Firestore / Cloud Platform access
        List<String> scopes = List.of(
                "https://www.googleapis.com/auth/datastore",
                "https://www.googleapis.com/auth/cloud-platform"
        );

        try (FileInputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
            GoogleCredentials creds = GoogleCredentials.fromStream(serviceAccount).createScoped(scopes);
            // Ensure token is available / fresh
            creds.refreshIfExpired();
            AccessToken token = creds.getAccessToken();
            if (token == null) {
                creds.refresh();
                token = creds.getAccessToken();
            }
            if (token == null) return null;
            return token.getTokenValue();
        }
    }
}
