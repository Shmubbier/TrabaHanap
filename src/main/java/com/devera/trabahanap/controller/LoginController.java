package com.devera.trabahanap.controller;

import com.devera.trabahanap.system.FirebaseInitializer;
import com.devera.trabahanap.system.SessionManager;
import com.devera.trabahanap.system.Config;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * LoginController: handles the login form and performs Firebase Auth REST sign-in.
 */
public class LoginController extends Controller {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    // Simple in-memory holder for current session token (move to dedicated session manager as needed)
    public static class Session {
        public static String idToken;
        public static String localId;
    }

    private static final Gson gson = new Gson();

    @FXML
    private void onLoginClicked() {
        String email = emailField.getText();
        String password = passwordField.getText();

        System.out.println("[LoginController] Login clicked. Email: " + email + " Password: " + (password == null ? "<null>" : "<redacted>"));

        // Initialize Firebase Admin (optional for other server operations)
        try {
            FirebaseInitializer.ensureInitialized();
        } catch (RuntimeException e) {
            // Log initialization problems but continue — REST sign-in only needs the web API key
            e.printStackTrace();
        }

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            System.err.println("[LoginController] Email or password blank.");
            // TODO: show a user-facing error dialog
            return;
        }

        // Read the Firebase Web API Key from configuration
        String apiKey = Config.get("firebase.webApiKey");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("[LoginController] Firebase webApiKey missing in configuration (firebase.webApiKey).");
            // TODO: show a user-facing error dialog
            return;
        }

        // Run network call asynchronously to avoid blocking JavaFX thread
        final String finalApiKey = apiKey;
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return signInWithEmailAndPassword(finalApiKey, email, password);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).whenComplete((success, throwable) -> {
            javafx.application.Platform.runLater(() -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Sign-in error");
                    alert.setHeaderText("Network or unexpected error");
                    alert.setContentText(throwable.getCause() != null ? throwable.getCause().getMessage() : throwable.getMessage());
                    alert.showAndWait();
                    return;
                }

                if (Boolean.TRUE.equals(success)) {
                    // on success navigate to Home
                    try {
                        navigate("/fxml/Home.fxml");
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                        alert.setTitle("Navigation error");
                        alert.setHeaderText("Could not open Home");
                        alert.setContentText(e.getMessage());
                        alert.showAndWait();
                    }
                } else {
                    // authentication failed - show friendly message
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                    alert.setTitle("Authentication failed");
                    alert.setHeaderText("Invalid credentials");
                    alert.setContentText("Email or password is incorrect. Please try again.");
                    alert.showAndWait();
                }
            });
        });
    }

    /**
     * Performs Firebase Auth REST signInWithPassword request.
     *
     * On success stores idToken and localId in Session and returns true.
     *
     * @param apiKey  Firebase Web API Key (from project Settings -> Web API Key)
     * @param email   user email
     * @param password user password
     * @return true when authentication succeeded
     */
    private boolean signInWithEmailAndPassword(String apiKey, String email, String password) throws IOException, InterruptedException {
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apiKey;

        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", password);
        payload.addProperty("returnSecureToken", true);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        int status = response.statusCode();
        String body = response.body();
        if (status == 200) {
            try {
                JsonObject json = gson.fromJson(body, JsonObject.class);
                String idToken = json.has("idToken") ? json.get("idToken").getAsString() : null;
                String localId = json.has("localId") ? json.get("localId").getAsString() : null;
                String emailResp = json.has("email") ? json.get("email").getAsString() : email; // fallback to entered email
                // store in SessionManager instead of static Session
                SessionManager.get().setSession(idToken, localId, emailResp, null);
                System.out.println("[LoginController] Authentication succeeded. localId=" + localId);
                return true;
            } catch (JsonParseException ex) {
                System.err.println("[LoginController] Failed to parse success response: " + ex.getMessage());
                return false;
            }
        } else {
            // Try to parse error details
            try {
                JsonObject err = gson.fromJson(body, JsonObject.class);
                if (err != null && err.has("error")) {
                    JsonObject e = err.getAsJsonObject("error");
                    String message = e.has("message") ? e.get("message").getAsString() : e.toString();
                    System.err.println("[LoginController] Sign-in error: " + message);
                } else {
                    System.err.println("[LoginController] Sign-in failed. HTTP " + status + ". Body: " + body);
                }
            } catch (JsonParseException ex) {
                System.err.println("[LoginController] Sign-in failed. HTTP " + status + ". Body: " + body);
            }
            return false;
        }
    }
}
