package com.devera.trabahanap.controller;

import com.devera.trabahanap.service.FirebaseUserService;
import com.devera.trabahanap.system.Config;
import com.devera.trabahanap.system.SessionManager;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;

/**
 * Controller for Home.fxml: displays user info and handles logout.
 */
public class HomeController extends Controller {

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userStatusLabel;

    @FXML
    private Button logoutButton;

    private final FirebaseUserService userService = new FirebaseUserService();

    @FXML
    public void initialize() {
        // Populate UI from session manager immediately if available
        SessionManager sm = SessionManager.get();
        sm.getEmail().ifPresent(email -> {
            Platform.runLater(() -> {
                userNameLabel.setText(email);
            });
        });

        // Optionally fetch a fresh profile using idToken
        String apiKey = Config.get("firebase.webApiKey");
        sm.getIdToken().ifPresent(idToken -> {
            if (apiKey != null && !apiKey.isBlank()) {
                userService.getUserProfile(apiKey, idToken).whenComplete((json, thr) -> {
                    Platform.runLater(() -> {
                        if (thr != null) {
                            showAlert("Profile error", "Could not fetch user profile: " + thr.getMessage(), Alert.AlertType.WARNING);
                            return;
                        }
                        // parse displayName/email/uid
                        String displayName = json.has("displayName") ? json.get("displayName").getAsString() : null;
                        String email = json.has("email") ? json.get("email").getAsString() : null;
                        if (displayName != null && !displayName.isBlank()) {
                            userNameLabel.setText(displayName);
                        } else if (email != null) {
                            userNameLabel.setText(email);
                        }
                        // Could set userStatusLabel or other UI elements here
                    });
                });
            }
        });
    }

    @FXML
    private void onLogoutClicked() {
        // Clear session and navigate back to login
        SessionManager.get().clear();
        try {
            navigate("/fxml/Login.fxml");
        } catch (IOException e) {
            showAlert("Navigation error", "Failed to return to login: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
