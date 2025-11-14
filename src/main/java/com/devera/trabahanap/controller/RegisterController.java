package com.devera.trabahanap.controller;

import com.devera.trabahanap.system.Config;
import com.devera.trabahanap.system.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;


 // Controller for Register_Page.fxml
public class RegisterController extends Controller {

    @FXML
    private TextField registerUsernameField;
    @FXML
    private TextField registerEmailAddressField;
    @FXML
    private PasswordField registerPasswordField;
    @FXML
    private PasswordField registerConfirmPasswordField;
    @FXML
    private Button registerSignUpBtn;

    private static final Gson gson = new Gson();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.]+@[A-Za-z0-9.-]+$");
    private static final int MIN_PASSWORD_LENGTH = 6;


    @FXML
    private void onSignInClicked() {
        try {
            navigate("/fxml/Login_Page.fxml");
        } catch (Exception e) {
            showErrorAlert("Navigation error", "Unable to open login page", e.getMessage());
        }
    }

    private void onSignUpClicked() {
        String username = safeGet(registerUsernameField);
        String email = safeGet(registerEmailAddressField);
        String password = safeGet(registerPasswordField);
        String confirmPassword = safeGet(registerConfirmPasswordField);

        if (!validateInputs(username, email, password, confirmPassword)) return;

        String apiKey = Config.get("firebase.webApiKey");
        if (apiKey == null || apiKey.isBlank()) {
            showErrorAlert("Config Error", "Firebase Key Missing", "Please set firebase.webApiKey");
            return;
        }

        registerSignUpBtn.setDisable(true);

        CompletableFuture.supplyAsync(() -> {
            try {
                return signUpWithEmailAndPassword(apiKey, email, password, username);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).whenComplete((success, throwable) -> {
            Platform.runLater(() -> {
                registerSignUpBtn.setDisable(false);

                if (throwable != null) {
                    showErrorAlert("Sign-up error", "Unexpected error", throwable.getMessage());
                    return;
                }

                if (success) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Registration Successful");
                    alert.setHeaderText("Welcome to TrabaHanap!");
                    alert.setContentText("Account created successfully.");
                    alert.showAndWait();

                    try {
                        navigate("/fxml/Home.fxml");
                    } catch (IOException e) {
                        showErrorAlert("Navigation error", "Could not open Home", e.getMessage());
                    }
                } else {
                    showErrorAlert("Registration Failed", "Could not create account", "Try again.");
                }
            });
        });
    }

    private boolean validateInputs(String username, String email, String password, String confirmPassword) {
        if (username.isBlank()) {
            showWarningAlert("Username Required", "Enter a username.");
            return false;
        }

        if (email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            showWarningAlert("Invalid Email", "Enter a valid email.");
            return false;
        }

        if (password.isBlank() || password.length() < MIN_PASSWORD_LENGTH) {
            showWarningAlert("Weak Password", "Minimum " + MIN_PASSWORD_LENGTH + " characters.");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            showWarningAlert("Passwords Mismatch", "Passwords must match.");
            return false;
        }

        return true;
    }

    private boolean signUpWithEmailAndPassword(String apiKey, String email, String password, String displayName) throws IOException, InterruptedException {
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + apiKey;

        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", password);
        payload.addProperty("returnSecureToken", true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            System.err.println("SignUp Error: " + response.body());
            return false;
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        String idToken = json.get("idToken").getAsString();
        String localId = json.get("localId").getAsString();

        SessionManager.get().setSession(idToken, localId, email, Instant.now().plusSeconds(3600));
        SessionManager.get().setDisplayName(displayName);

        // Firestore-only: write user profile to Firestore users/{uid}.
        // If this write fails, propagate as IOException so caller can handle cleanup (delete account) if desired.
        try {
            com.devera.trabahanap.service.FirestoreService fs = new com.devera.trabahanap.service.FirestoreService();
            fs.writeUserProfile(idToken, localId, displayName, email)
              .exceptionally(th -> {
                  // wrap to surface as checked exception below
                  throw new RuntimeException("Failed to write user profile to Firestore: " + th.getMessage(), th);
              }).join();
        } catch (RuntimeException rte) {
            Throwable cause = rte.getCause() != null ? rte.getCause() : rte;
            if (cause instanceof IOException) throw (IOException) cause;
            if (cause instanceof InterruptedException) throw (InterruptedException) cause;
            throw new IOException("Failed to write user profile to Firestore: " + rte.getMessage(), rte);
        }

        updateDisplayNameAsync(apiKey, idToken, displayName);

        // --------------------------
        // New: write user profile to Firestore users/{uid}
        // --------------------------
        try {
            // FirestoreService invoked asynchronously but we will wait short time to ensure write attempted.
            com.devera.trabahanap.service.FirestoreService fs = new com.devera.trabahanap.service.FirestoreService();
            fs.writeUserProfile(idToken, localId, displayName, email)
                    .exceptionally(th -> {
                        // Log the error but do not fail the whole signup flow if Firestore write fails.
                        System.err.println("[RegisterController] Firestore user write failed: " + th.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            // non-fatal; continue
            System.err.println("[RegisterController] Failed to initialize FirestoreService: " + e.getMessage());
        }

        return true;
    }

    /**
     * Firestore-only: write user profile into Firestore users/{uid}.
     * <p>
     * Kept for compatibility with callers — always returns true on success.
     */
    private boolean checkUsernameUniqueAndWriteToDatabase(String idToken, String uid, String displayName, String email) throws IOException, InterruptedException {
        // Realtime DB removed — write user record directly to Firestore.
        try {
            com.devera.trabahanap.service.FirestoreService fs = new com.devera.trabahanap.service.FirestoreService();
            fs.writeUserProfile(idToken, uid, displayName, email)
                    .exceptionally(th -> {
                        // convert to checked exception for caller
                        throw new RuntimeException("Failed to write user profile to Firestore: " + th.getMessage(), th);
                    }).join();
            return true;
        } catch (RuntimeException rte) {
            Throwable cause = rte.getCause() != null ? rte.getCause() : rte;
            if (cause instanceof IOException) throw (IOException) cause;
            if (cause instanceof InterruptedException) throw (InterruptedException) cause;
            throw new IOException("Failed to write user profile to Firestore: " + rte.getMessage(), rte);
        }
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarningAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String safeGet(TextField field) {
        return field.getText() == null ? "" : field.getText();
    }

    /**
     * Deletes the currently authenticated user using Firebase Identity Toolkit accounts:delete.
     * Requires a valid idToken for that user.
     */
    private void deleteAccount(String apiKey, String idToken) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) return;

        String url = "https://identitytoolkit.googleapis.com/v1/accounts:delete?key=" + apiKey;
        JsonObject payload = new JsonObject();
        payload.addProperty("idToken", idToken);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /**
     * Updates the Firebase Authentication user's displayName asynchronously.
     * Non-critical: failures are ignored.
     */
    private void updateDisplayNameAsync(String apiKey, String idToken, String displayName) {
        CompletableFuture.runAsync(() -> {
            try {
                String url = "https://identitytoolkit.googleapis.com/v1/accounts:update?key=" + apiKey;

                JsonObject payload = new JsonObject();
                payload.addProperty("idToken", idToken);
                payload.addProperty("displayName", displayName);
                payload.addProperty("returnSecureToken", true);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                        .build();

                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            } catch (Exception ignored) {
            }
        });
    }
}
