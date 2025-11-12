package com.devera.trabahanap.system;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

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

    // New: listener for displayName updates
    private Consumer<String> displayNameListener;

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
        this.displayNameListener = null; // clear listener too
    }

    // New helper to set user-identifying fields (used by LoginController)
    public synchronized void setUserSession(String firebaseUserId, String userEmail, String userDisplayName) {
        this.localId = firebaseUserId;
        this.email = userEmail;
        this.displayName = userDisplayName;
        notifyDisplayNameListener(userDisplayName);
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

    // Display name accessors
    public synchronized Optional<String> getDisplayName() {
        return Optional.ofNullable(displayName);
    }

    public synchronized void setDisplayName(String name) {
        this.displayName = name;
        notifyDisplayNameListener(name);
    }

    // ---- NEW CODE BELOW ----
    // Register listener for when displayName changes
    public synchronized void addDisplayNameListener(Consumer<String> listener) {
        this.displayNameListener = listener;
        // Immediately notify if displayName is already available
        if (this.displayName != null) {
            listener.accept(this.displayName);
        }
    }

    // Notify listener if one is set
    private synchronized void notifyDisplayNameListener(String name) {
        if (displayNameListener != null && name != null) {
            displayNameListener.accept(name);
        }
    }
}
