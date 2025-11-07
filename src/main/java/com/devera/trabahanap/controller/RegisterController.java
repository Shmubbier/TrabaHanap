package com.devera.trabahanap.controller;

import javafx.fxml.FXML;

/**
 * Controller for Register_Page.fxml
 * Provides navigation back to the login page when the "Sign in" text is clicked.
 */
public class RegisterController extends Controller {

    @FXML
    private void onSignInClicked() {
        try {
            // Use the Controller.navigate helper to go back to the login page
            navigate("/fxml/Login_Page.fxml");
        } catch (java.io.IOException e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Navigation error");
            alert.setHeaderText("Could not open Login");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
