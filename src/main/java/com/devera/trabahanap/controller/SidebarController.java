package com.devera.trabahanap.controller;

import com.devera.trabahanap.system.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;
import java.util.Optional;

public class SidebarController extends Controller {

    private HomeController homeController;

    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    @FXML
    private void onHomeClick() {
        homeController.loadPage("Home_Content.fxml");
    }

    @FXML
    private void onBrowseClick() {
        homeController.loadPage("BrowseJob_Content.fxml");
    }

    @FXML
    private void onPostClick() {
        homeController.loadPage("PostJob.fxml");
    }

    @FXML
    private void onMessagesClick() {
        // TODO later
    }

    @FXML
    private void onProfileClick() {
        // TODO later
    }

    @FXML
    private Label sideAccountName; // bound to fx:id in Sidebar.fxml

    @FXML
    private void initialize() {
        // populate account name from SessionManager if available
        try {
            SessionManager.get().getDisplayName().ifPresentOrElse(
                    name -> sideAccountName.setText(name),
                    () -> SessionManager.get().getEmail().ifPresent(email -> sideAccountName.setText(email))
            );
        } catch (Exception e) {
            // fallback: keep current FXML default text if anything fails
            System.err.println("[SidebarController] Failed to populate account name: " + e.getMessage());
        }
    }

    @FXML
    private void onLogoutClick() {
        // Terminate session
        SessionManager.get().clear();

        // Confirm session cleared before redirecting
        if (!SessionManager.get().isAuthenticated()) {
            try {
                // Reuse Controller.navigate helper to navigate back to login page,
                // letting it resolve the active Stage.
                navigate("/fxml/Login_Page.fxml");
            } catch (java.io.IOException e) {
                e.printStackTrace();
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Logout error");
                alert.setHeaderText("Failed to return to login");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        } else {
            // Fallback: show error if session could not be cleared
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Logout error");
            alert.setHeaderText("Session termination failed");
            alert.setContentText("Could not clear the current session. Please try again.");
            alert.showAndWait();
        }
    }
}
