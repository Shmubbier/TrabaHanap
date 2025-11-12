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

/**
 * Controller for Register_Page.fxml
 */
public class RegisterController extends Controller {

    @FXML private TextField registerUsernameField;
    @FXML private TextField registerEmailAddressField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField registerConfirmPasswordField;
    @FXML private Button registerSignUpBtn;
    @FXML private Button registerGoogleSignIn;

    private static final Gson gson = new Gson();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.]+@[A-Za-z0-9.-]+$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    @FXML
    private void initialize() {
        registerSignUpBtn.setOnAction(event -> onSignUpClicked());
        registerGoogleSignIn.setDisable(true);
    }

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

        // Check uniqueness and write to Realtime Database. If username already taken, delete created account and fail.
        try {
            boolean dbOk = checkUsernameUniqueAndWriteToDatabase(idToken, localId, displayName, email);
            if (!dbOk) {
                // Attempt to remove the created auth user since username is taken
                deleteAccount(apiKey, idToken);
                return false;
            }
        } catch (Exception ex) {
            // If DB check/write fails, try to delete the created account to avoid partial state
            try { deleteAccount(apiKey, idToken); } catch (Exception ignored) {}
            throw ex;
        }

        updateDisplayNameAsync(apiKey, idToken, displayName);

        return true;
    }

    /**
     * Returns true if username is unique and user record was written successfully.
     * If username already exists returns false.
     */
    private boolean checkUsernameUniqueAndWriteToDatabase(String idToken, String uid, String displayName, String email) throws IOException, InterruptedException {
        String dbUrl = Config.get("firebase.databaseUrl");
        if (dbUrl == null || dbUrl.isBlank()) {
            throw new IllegalStateException("firebase.databaseUrl is not set in config");
        }

        // Normalize DB base url (ensure no trailing slash)
        if (dbUrl.endsWith("/")) dbUrl = dbUrl.substring(0, dbUrl.length() - 1);

        // Check if username exists: use orderBy="displayName"&equalTo="displayName"
        if (isUsernameTaken(dbUrl, idToken, displayName)) {
            return false;
        }

        // Write user record under /users/{uid}.json
        return writeUserRecord(dbUrl, idToken, uid, displayName, email);
    }

    private boolean isUsernameTaken(String dbUrl, String idToken, String displayName) throws IOException, InterruptedException {
        // orderBy and equalTo must be JSON-encoded strings; URLEncoder will percent-encode quotes too.
        String orderBy = URLEncoder.encode("\"displayName\"", StandardCharsets.UTF_8);
        String equalTo = URLEncoder.encode(gson.toJson(displayName), StandardCharsets.UTF_8); // adds quotes
        String query = String.format("%s/users.json?orderBy=%s&equalTo=%s&auth=%s", dbUrl, orderBy, equalTo, URLEncoder.encode(idToken, StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(query))
                .GET()
                .build();

        HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 400) {
            // treat as error to bubble up
            throw new IOException("Realtime DB query failed: " + resp.body());
        }

        // If response is an empty object {} -> no match. If it contains entries -> username taken.
        String body = resp.body();
        JsonObject parsed;
        try {
            parsed = gson.fromJson(body, JsonObject.class);
        } catch (JsonParseException e) {
            throw new IOException("Failed to parse Realtime DB response: " + body, e);
        }

        // If parsed is null or has no keys -> not taken
        return parsed != null && parsed.entrySet().size() > 0;
    }

    private boolean writeUserRecord(String dbUrl, String idToken, String uid, String displayName, String email) throws IOException, InterruptedException {
        String target = String.format("%s/users/%s.json?auth=%s", dbUrl, URLEncoder.encode(uid, StandardCharsets.UTF_8), URLEncoder.encode(idToken, StandardCharsets.UTF_8));

        JsonObject record = new JsonObject();
        record.addProperty("displayName", displayName);
        record.addProperty("email", email);
        record.addProperty("createdAt", Instant.now().toString());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(target))
                .header("Content-Type", "application/json; charset=UTF-8")
                .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(record)))
                .build();

        HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 400) {
            throw new IOException("Failed to write user record: " + resp.body());
        }
        return true;
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

                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            } catch (Exception ignored) {}
        });
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(header); alert.setContentText(content); alert.showAndWait();
    }

    private void showWarningAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning"); alert.setHeaderText(header); alert.setContentText(content); alert.showAndWait();
    }

    private String safeGet(TextField field) {
        return field.getText() == null ? "" : field.getText();
    }
}
