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
    private String displayName; // added to hold Firebase displayName locally

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
        this.displayName = null; // clear displayName on logout
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

    // New: displayName accessors
    public synchronized Optional<String> getDisplayName() {
        return Optional.ofNullable(displayName);
    }

    public synchronized void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    // TODO: add method to check expiry and refresh using refreshToken through Firebase token endpoints.
}
