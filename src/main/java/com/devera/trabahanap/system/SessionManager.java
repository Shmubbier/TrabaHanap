package com.devera.trabahanap.system;

import java.time.Instant;
import java.util.Optional;

/**
 * Simple session manager: stores current authenticated user's idToken, localId and email.
 * TODO: persist to secure storage, add token expiry/refresh logic using refreshToken.
 */
public final class SessionManager {

    private static volatile SessionManager instance = new SessionManager();

    private String idToken;
    private String localId;
    private String email;
    private Instant expiresAt; // optional, set when token expiry info is available

    private SessionManager() {}

    public static SessionManager get() {
        return instance;
    }

    public synchronized void setSession(String idToken, String localId, String email, Instant expiresAt) {
        this.idToken = idToken;
        this.localId = localId;
        this.email = email;
        this.expiresAt = expiresAt;
    }

    public synchronized void clear() {
        this.idToken = null;
        this.localId = null;
        this.email = null;
        this.expiresAt = null;
    }

    public synchronized Optional<String> getIdToken() {
        return Optional.ofNullable(idToken);
    }

    public synchronized Optional<String> getLocalId() {
        return Optional.ofNullable(localId);
    }

    public synchronized Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    public synchronized Optional<Instant> getExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    public synchronized boolean isAuthenticated() {
        return idToken != null && localId != null;
    }

    // TODO: add method to check expiry and refresh using refreshToken through Firebase token endpoints.
}
