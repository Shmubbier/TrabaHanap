package com.devera.trabahanap.controller;

import com.devera.trabahanap.system.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Window;
import javafx.stage.Stage;

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
    private void onSettingsClick() {
        // TODO later
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
            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Logout error");
                alert.setHeaderText("Failed to return to login");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        } else {
            // Fallback: show error if session could not be cleared
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Logout error");
            alert.setHeaderText("Session termination failed");
            alert.setContentText("Could not clear the current session. Please try again.");
            alert.showAndWait();
        }
    }
}
