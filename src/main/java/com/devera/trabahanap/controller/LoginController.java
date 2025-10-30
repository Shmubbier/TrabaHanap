package com.devera.trabahanap.controller;

import com.trabahanap.core.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * LoginController: handles the login form. For now it only prints credentials.
 */
public class LoginController extends Controller {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void onLoginClicked() {
        String email = emailField.getText();
        String password = passwordField.getText();

        // Debug output only (no Firebase calls yet)
        System.out.println("[LoginController] Login clicked. Email: " + email + " Password: " + (password == null ? "<null>" : "<redacted>"));

        // Future: validate and perform authentication; then navigate to home:
        // try { switchScene("/fxml/Home.fxml"); } catch (IOException e) { e.printStackTrace(); }
    }

    // Optionally override getStage() if you want to provide stage reference for BaseController navigation
}
