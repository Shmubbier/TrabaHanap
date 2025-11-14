package com.devera.trabahanap.controller;

import com.devera.trabahanap.core.Job;
import com.devera.trabahanap.service.FirebaseUserService;
import com.devera.trabahanap.service.JobService;
import com.devera.trabahanap.service.FirestoreService;
import com.devera.trabahanap.system.Config;
import com.devera.trabahanap.system.SessionManager;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * HomeController: loads sidebar, loads center pages, fetches jobs for homepage,
 * exposes navigation helper for content controllers.
 */
public class HomeController extends Controller {

    @FXML private Label userNameLabel;
    @FXML private Label userStatusLabel;
    @FXML private Button logoutButton;

    @FXML private HBox jobsHBox; // used for homepage trending cards (optional - populated at initialize)
    @FXML private BorderPane borderPane;

    private final JobService jobService = new JobService();
    private final FirebaseUserService userService = new FirebaseUserService();

    @FXML
    public void initialize() {
        // load sidebar and inject this controller
        try {
            FXMLLoader sidebarLoader = new FXMLLoader(getClass().getResource("/fxml/Sidebar.fxml"));
            Node sidebar = sidebarLoader.load();
            SidebarController sidebarController = sidebarLoader.getController();
            if (sidebarController != null) sidebarController.setHomeController(this);
            if (borderPane != null) borderPane.setLeft(sidebar);
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadPage("Home_Content.fxml");

        // Fill user info if any
        SessionManager sm = SessionManager.get();
        sm.getEmail().ifPresent(email -> Platform.runLater(() -> userNameLabel.setText(email)));

        String apiKey = Config.get("firebase.webApiKey");
        sm.getIdToken().ifPresent(idToken -> {
            if (apiKey != null && !apiKey.isBlank()) {
                userService.getUserProfile(apiKey, idToken).whenComplete((json, thr) -> {
                    Platform.runLater(() -> {
                        if (thr != null) {
                            showAlert("Profile error", "Could not fetch user profile: " + thr.getMessage(), Alert.AlertType.WARNING);
                            return;
                        }
                        String displayName = json.has("displayName") ? json.get("displayName").getAsString() : null;
                        String email = json.has("email") ? json.get("email").getAsString() : null;
                        String uid = json.has("localId") ? json.get("localId").getAsString() : null;

                        if (displayName != null && !displayName.isBlank()) userNameLabel.setText(displayName);
                        else if (email != null) userNameLabel.setText(email);

                        if (uid != null && !uid.isBlank()) {
                            try {
                                FirestoreService fs = new FirestoreService();
                                fs.getUserProfileDocument(idToken, uid).whenComplete((maybeFields, err) -> {
                                    Platform.runLater(() -> {
                                        if (err != null) return;
                                        maybeFields.ifPresent(fields -> {
                                            if (fields.has("displayName")) {
                                                String dn = fields.get("displayName").getAsJsonObject().get("stringValue").getAsString();
                                                if (dn != null && !dn.isBlank()) {
                                                    userNameLabel.setText(dn);
                                                    SessionManager.get().setDisplayName(dn);
                                                }
                                            } else if (fields.has("email") && SessionManager.get().getDisplayName().isEmpty()) {
                                                String em = fields.get("email").getAsJsonObject().get("stringValue").getAsString();
                                                userNameLabel.setText(em);
                                                SessionManager.get().setDisplayName(em);
                                            }
                                        });
                                    });
                                });
                            } catch (Exception e) {
                                System.err.println("[HomeController] FirestoreService error: " + e.getMessage());
                            }
                        }
                    });
                });
            }
        });

        // render homepage small list if jobsHBox present (optional)
        fetchAndRenderJobs();
    }

    public BorderPane getBorderPane() {
        return borderPane;
    }


    /**
     * Fetch jobs and add JobCard nodes into jobsHBox (used on Home page).
     */
    private void fetchAndRenderJobs() {
        if (jobsHBox != null) Platform.runLater(jobsHBox.getChildren()::clear);

        jobService.getAllJobs().whenComplete((jobs, thr) -> {
            Platform.runLater(() -> {
                if (thr != null) {
                    showAlert("Jobs error", "Failed to load jobs: " + thr.getMessage(), Alert.AlertType.ERROR);
                    return;
                }
                if (jobs == null || jobs.isEmpty() || jobsHBox == null) return;

                // limit to 6 trending cards
                int max = Math.min(6, jobs.size());
                for (int i = 0; i < max; i++) {
                    Job j = jobs.get(i);
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/JobCard.fxml"));
                        Node card = loader.load();
                        Object c = loader.getController();
                        if (c instanceof JobCardController) {
                            ((JobCardController) c).setJob(j);
                        }
                        card.setOnMouseClicked(ev -> openJobDetails(j));
                        jobsHBox.getChildren().add(card);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }

    /**
     * Public navigation helper that other controllers can call.
     * Opens JobDetails and passes the Job to the details controller.
     */
    public void openJobDetails(Job job) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/JobDetails.fxml"));
            Parent detailsRoot = loader.load();
            JobDetailsController controller = loader.getController();
            controller.setHomeController(this);
            controller.setJob(job); // populate data

            borderPane.setCenter(detailsRoot); // show in main layout
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onLogoutClicked() {
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

    /**
     * Load content into the center of the BorderPane. If the loaded controller defines
     * setHomeController(HomeController) it will be injected automatically.
     */
    public void loadPage(String fxmlName) {
        if (borderPane == null) {
            System.err.println("[HomeController] borderPane is null, cannot load page: " + fxmlName);
            return;
        }
        String path = fxmlName.startsWith("/") ? fxmlName : ("/fxml/" + fxmlName);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Node content = loader.load();
            Object controller = loader.getController();
            if (controller != null) {
                try {
                    controller.getClass().getMethod("setHomeController", HomeController.class).invoke(controller, this);
                } catch (NoSuchMethodException ignored) {
                } catch (ReflectiveOperationException roe) {
                    System.err.println("[HomeController] Failed to inject HomeController: " + roe.getMessage());
                }
            }
            borderPane.setCenter(content);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Load error", "Could not load page: " + fxmlName + " — " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}
