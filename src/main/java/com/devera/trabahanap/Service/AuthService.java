package com.trabahanap.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.trabahanap.system.FirebaseInitializer;

import java.util.concurrent.CompletableFuture;

/**
 * AuthService: wraps Firebase Auth operations. Uses FirebaseInitializer to ensure SDK is initialized.
 * Note: For production, handle tokens, user sessions and error mapping properly.
 */
public class AuthService {

    private final FirebaseAuth auth;

    public AuthService() {
        FirebaseInitializer.ensureInitialized();
        this.auth = FirebaseAuth.getInstance();
    }

    public CompletableFuture<UserRecord> registerUser(String email, String password, String displayName) {
        return CompletableFuture.supplyAsync(() -> {
            UserRecord.CreateRequest req = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setDisplayName(displayName);
            try {
                return auth.createUser(req);
            } catch (FirebaseAuthException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<UserRecord> getUserByEmail(String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return auth.getUserByEmail(email);
            } catch (FirebaseAuthException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> deleteUser(String uid) {
        return CompletableFuture.runAsync(() -> {
            try {
                auth.deleteUser(uid);
            } catch (FirebaseAuthException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // Note: firebase-admin doesn't provide client-like sign-in (returns tokens for verifying).
    // For client authentication flows, use a client SDK or custom token approach.
}
