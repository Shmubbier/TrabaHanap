package com.devera.trabahanap.controller;

import com.devera.trabahanap.system.Config;
import com.devera.trabahanap.system.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Controller for Register_Page.fxml
 * Handles user registration via Firebase Auth REST API.
 */
public class RegisterController extends Controller {

    @FXML
    private TextField registerUsernameField;

    @FXML
    private TextField registerEmailAddressField;

    // corrected field name to match FXML fx:id
    @FXML
    private PasswordField registerPasswordField;

    @FXML
    private PasswordField registerConfirmPasswordField;

    @FXML
    private Button registerSignUpBtn;

    // Google sign-in button placeholder (disabled)
    @FXML
    private Button registerGoogleSignIn;

    private static final Gson gson = new Gson();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    @FXML
    private void initialize() {
        // Bind sign-up button action
        if (registerSignUpBtn != null) {
            registerSignUpBtn.setOnAction(event -> onSignUpClicked());
        }

        // Disable Google button placeholder until implemented
        if (registerGoogleSignIn != null) {
            registerGoogleSignIn.setDisable(true);
        }
    }

    @FXML
    private void onSignInClicked() {
        try {
            // Use the Controller.navigate helper to go back to the login page
            navigate("/fxml/Login_Page.fxml");
        } catch (java.io.IOException e) {
            e.printStackTrace();
            showErrorAlert("Navigation error", "Could not open Login", e.getMessage());
        }
    }

    private void onSignUpClicked() {
        String username = safeGet(registerUsernameField);
        String email = safeGet(registerEmailAddressField);
        String password = safeGet(registerPasswordField);
        String confirmPassword = safeGet(registerConfirmPasswordField);

        System.out.println("[RegisterController] Sign-up clicked. Username: " + username + ", Email: " + email);

        // Validation
        if (!validateInputs(username, email, password, confirmPassword)) {
            return; // Validation failed, alert already shown
        }

        // Read Firebase Web API Key
        String apiKey = Config.get("firebase.webApiKey");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("[RegisterController] Firebase webApiKey missing in configuration.");
            showErrorAlert("Configuration Error", "Firebase API Key Missing", "Please configure firebase.webApiKey in config.properties");
            return;
        }

        // Disable button to prevent double-click
        registerSignUpBtn.setDisable(true);

        // Perform registration asynchronously
        CompletableFuture.supplyAsync(() -> {
            try {
                return signUpWithEmailAndPassword(apiKey, email, password, username);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).whenComplete((success, throwable) -> {
            Platform.runLater(() -> {
                registerSignUpBtn.setDisable(false); // Re-enable button

                if (throwable != null) {
                    throwable.printStackTrace();
                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    showErrorAlert("Sign-up error", "Network or unexpected error", cause.getMessage());
                    return;
                }

                if (Boolean.TRUE.equals(success)) {
                    // Show success message
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Registration Successful");
                    alert.setHeaderText("Welcome to TrabaHanap!");
                    alert.setContentText("Your account has been created successfully. You are now logged in.");
                    alert.showAndWait();

                    // Navigate to Home (user is auto-logged in)
                    try {
                        navigate("/fxml/Home.fxml");
                    } catch (IOException e) {
                        e.printStackTrace();
                        showErrorAlert("Navigation error", "Could not open Home", e.getMessage());
                    }
                } else {
                    // Registration failed
                    showErrorAlert("Registration Failed", "Could not create account", "Please check your details and try again.");
                }
            });
        });
    }

    /**
     * Validates user inputs with UI feedback.
     *
     * @return true if all inputs are valid
     */
    private boolean validateInputs(String username, String email, String password, String confirmPassword) {
        // Username validation
        if (username == null || username.isBlank()) {
            showWarningAlert("Validation Error", "Username Required", "Please enter a username.");
            return false;
        }

        // Email validation
        if (email == null || email.isBlank()) {
            showWarningAlert("Validation Error", "Email Required", "Please enter your email address.");
            return false;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showWarningAlert("Validation Error", "Invalid Email Format", "Please enter a valid email address (e.g., user@example.com).");
            return false;
        }

        // Password validation
        if (password == null || password.isBlank()) {
            showWarningAlert("Validation Error", "Password Required", "Please enter a password.");
            return false;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            showWarningAlert("Validation Error", "Password Too Short", "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
            return false;
        }

        // Confirm password validation
        if (confirmPassword == null || confirmPassword.isBlank()) {
            showWarningAlert("Validation Error", "Confirm Password Required", "Please confirm your password.");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            showWarningAlert("Validation Error", "Passwords Do Not Match", "Password and Confirm Password must be identical.");
            return false;
        }

        return true;
    }

    /**
     * Performs Firebase Auth REST signUp request.
     *
     * On success stores idToken, localId, and email in SessionManager and returns true.
     *
     * @param apiKey      Firebase Web API Key
     * @param email       user email
     * @param password    user password
     * @param displayName user display name
     * @return true when registration succeeded
     */
    private boolean signUpWithEmailAndPassword(String apiKey, String email, String password, String displayName) throws IOException, InterruptedException {
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + apiKey;

        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", password);
        payload.addProperty("displayName", displayName);
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
                String refreshToken = json.has("refreshToken") ? json.get("refreshToken").getAsString() : null;
                String localId = json.has("localId") ? json.get("localId").getAsString() : null;
                String emailResp = json.has("email") ? json.get("email").getAsString() : email;

                // Store session (auto-login)
                // SessionManager.setSession(idToken, localId, email, expiresAt)
                SessionManager.get().setSession(idToken, localId, emailResp, Instant.now().plusSeconds(3600)); // approximate expiry

                System.out.println("[RegisterController] Registration succeeded. localId=" + localId + ", refreshToken=" + (refreshToken != null ? "<present>" : "<null>"));
                // TODO: persist refreshToken securely for token refresh
                return true;
            } catch (JsonParseException ex) {
                System.err.println("[RegisterController] Failed to parse success response: " + ex.getMessage());
                return false;
            }
        } else {
            // Try to parse error details
            try {
                JsonObject err = gson.fromJson(body, JsonObject.class);
                if (err != null && err.has("error")) {
                    JsonObject e = err.getAsJsonObject("error");
                    String message = e.has("message") ? e.get("message").getAsString() : e.toString();
                    System.err.println("[RegisterController] Sign-up error: " + message);
                    // show friendly message to user
                    showErrorAlert("Registration Error", "Could not create account", message);
                } else {
                    System.err.println("[RegisterController] Sign-up failed. HTTP " + status + ". Body: " + body);
                    showErrorAlert("Registration Error", "Could not create account", "HTTP " + status);
                }
            } catch (JsonParseException ex) {
                System.err.println("[RegisterController] Sign-up failed. HTTP " + status + ". Body: " + body);
                showErrorAlert("Registration Error", "Could not create account", "HTTP " + status);
            }
            return false;
        }
    }

    // Helper methods for showing alerts
    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarningAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String safeGet(TextField f) {
        return f == null ? "" : f.getText();
    }
}
