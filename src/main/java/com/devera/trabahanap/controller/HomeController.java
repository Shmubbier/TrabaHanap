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
import com.devera.trabahanap.core.Job;
import com.devera.trabahanap.service.JobService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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

    // Container in Home.fxml where job cards will be added dynamically.
    // NOTE: small FXML change required: assign fx:id="jobsHBox" to the HBox that currently includes JobCard.fxml entries
    @FXML
    private HBox jobsHBox;

    private final JobService jobService = new JobService();

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

        // Fetch jobs asynchronously and populate job cards
       fetchAndRenderJobs();
    }

    /**
     * Fetch jobs from Firestore and add JobCard nodes into jobsHBox.
     */
    private void fetchAndRenderJobs() {
        // Clear any existing cards while loading
        if (jobsHBox != null) {
            Platform.runLater(() -> jobsHBox.getChildren().clear());
        }

        jobService.getAllJobs().whenComplete((jobs, thr) -> {
            Platform.runLater(() -> {
                if (thr != null) {
                    showAlert("Jobs error", "Failed to load jobs: " + thr.getMessage(), Alert.AlertType.ERROR);
                    return;
                }

                if (jobs == null || jobs.isEmpty()) {
                    // Optionally show a placeholder card or message
                    return;
                }

                for (Job j : jobs) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/JobCard.fxml"));
                        Node card = loader.load();
                        // The controller should expose setJob(Job)
                        Object controller = loader.getController();
                        try {
                            // Use reflection to call setJob if method exists to avoid compile-time dependency
                            controller.getClass().getMethod("setJob", Job.class).invoke(controller, j);
                        } catch (ReflectiveOperationException roe) {
                            // ignore - controller may not implement setJob or method couldn't be invoked
                        }

                        // Attach click listener to open details. JobCardController may also handle clicks,
                        // this is an extra safe hook to ensure navigation works.
                        card.setOnMouseClicked(ev -> {
                            openJobDetails(j);
                        });

                        jobsHBox.getChildren().add(card);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }

    /**
     * Open the JobDetails view and pass the selected Job to its controller.
     */
    private void openJobDetails(Job job) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/JobDetails.fxml"));
                Parent root = loader.load();
                Object controller = loader.getController();
                try {
                    controller.getClass().getMethod("setJob", Job.class).invoke(controller, job);
                } catch (ReflectiveOperationException roe) {
                    // controller missing setJob or invocation failed - log and continue
                    System.err.println("[HomeController] Could not call setJob(Job) on JobDetails controller: " + roe.getMessage());
                }

                // Replace current scene root while preserving scene and styles
                Stage stage = (Stage) userNameLabel.getScene().getWindow();
                Scene scene = stage.getScene();
                if (scene == null) {
                    stage.setScene(new Scene(root));
                } else {
                    scene.setRoot(root);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Navigation error", "Could not open job details: " + ex.getMessage(), Alert.AlertType.ERROR);
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
