package com.devera.trabahanap.controller;

import com.devera.trabahanap.service.FirebaseUserService;
import com.devera.trabahanap.system.Config;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.io.IOException;

public class ForgotPasswordController extends Controller {

    @FXML private TextField forgotEmailAddressField;
    @FXML private Label forgotEmailAlert;
    @FXML private Button forgotSubmitBtn;
    @FXML private Text forgotLoginLink;

    private final FirebaseUserService userService = new FirebaseUserService();

    @FXML
    public void initialize() {
        // Set action for submit button
        forgotSubmitBtn.setOnAction(e -> handleSubmit());

        // Set action for login link
        forgotLoginLink.setOnMouseClicked(e -> goBackToLogin());
    }

    private void handleSubmit() {
        String email = forgotEmailAddressField.getText().trim();

        if (email.isEmpty()) {
            showAlert("Please enter your email.");
            return;
        }

        if (!isValidEmail(email)) {
            showAlert("Invalid email format.");
            return;
        }

        String apiKey = Config.get("firebase.webApiKey");
        if (apiKey == null || apiKey.isBlank()) {
            showAlert("Missing Firebase API key in Config.");
            return;
        }

        forgotSubmitBtn.setDisable(true);

        userService.sendPasswordResetEmail(apiKey, email)
                .whenComplete((ok, err) -> Platform.runLater(() -> {
                    forgotSubmitBtn.setDisable(false);

                    if (err != null) {
                        showAlert("Failed: " + err.getMessage());
                        return;
                    }

                    showSuccess("A reset link has been sent to " + email);
                }));
    }

    private void showAlert(String msg) {
        forgotEmailAlert.setText(msg);
        forgotEmailAlert.setStyle("-fx-text-fill: red;");
        forgotEmailAlert.setVisible(true);
    }

    private void showSuccess(String msg) {
        forgotEmailAlert.setText(msg);
        forgotEmailAlert.setStyle("-fx-text-fill: green;");
        forgotEmailAlert.setVisible(true);
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private void goBackToLogin() {
        try {
            navigate("/fxml/Login_Page.fxml");
        } catch (IOException e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR
            );
            alert.setTitle("Navigation error");
            alert.setHeaderText("Could not open Login Page");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
