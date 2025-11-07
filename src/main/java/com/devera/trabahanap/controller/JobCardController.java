package com.devera.trabahanap.controller;

import com.devera.trabahanap.core.Job;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 *
 *
 * Controller for JobCard.fxml — handles hover effects and other card interactions.
 */
public class JobCardController extends Controller {

    @FXML
    private Button cardButton;

    // These fx:id fields should exist (or be added) in JobCard.fxml to display job info.
    @FXML
    private Label titleLabel;

    @FXML
    private Label companyLabel;

    @FXML
    private Label locationLabel;

    @FXML
    private Label salaryLabel;

    @FXML
    private Label shortDescLabel;

    // Keep reference to Job for click/navigation actions
    private Job job;

    @FXML
    private void onCardHover() {
        if (cardButton != null) {
            cardButton.setStyle("-fx-background-color: transparent; -fx-border-color: #555; -fx-border-width: 1;");
        }
    }

    @FXML
    private void onCardExit() {
        if (cardButton != null) {
            cardButton.setStyle("-fx-background-color: transparent; -fx-border-color: #999; -fx-border-width: 1;");
        }
    }

    /**
     * Populate the card with job data.
     * This method is expected to be called right after FXMLLoader.load().
     */
    public void setJob(Job job) {
        this.job = job;
        if (job == null) return;

        Platform.runLater(() -> {
            if (titleLabel != null) titleLabel.setText(job.getTitle() != null ? job.getTitle() : "(No title)");
            if (companyLabel != null) companyLabel.setText(job.getCompanyName() != null ? job.getCompanyName() : "");
            if (locationLabel != null) locationLabel.setText(job.getLocation() != null ? job.getLocation() : "");
            if (salaryLabel != null) salaryLabel.setText(job.getSalaryRange() != null ? job.getSalaryRange() : "");
            if (shortDescLabel != null) {
                String desc = job.getDescription() != null ? job.getDescription() : "";
                shortDescLabel.setText(desc.length() > 120 ? desc.substring(0, 117) + "..." : desc);
            }
        });
    }

    /**
     * Called when the card is clicked (wired via onAction of the card button or via onMouseClicked on the root).
     * Loads JobDetails.fxml and passes the Job object to its controller.
     */
    @FXML
    private void onCardClicked() {
        if (this.job == null) return;

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/JobDetails.fxml"));
                Parent root = loader.load();

                Object controller = loader.getController();
                try {
                    controller.getClass().getMethod("setJob", Job.class).invoke(controller, this.job);
                } catch (NoSuchMethodException nsme) {
                    System.err.println("[JobCardController] JobDetails controller does not implement setJob(Job)");
                }

                // Replace current scene root (keeps stylesheets)
                Stage stage = (Stage) (cardButton != null ? cardButton.getScene().getWindow() : null);
                if (stage == null) {
                    // try any window
                    stage = (Stage) Stage.getWindows().stream().filter(javafx.stage.Window::isShowing).findFirst().orElse(null);
                }

                if (stage != null) {
                    Scene scene = stage.getScene();
                    if (scene == null) {
                        stage.setScene(new Scene(root));
                    } else {
                        scene.setRoot(root);
                    }
                } else {
                    // Fallback: open in new window
                    Stage newStage = new Stage();
                    newStage.setScene(new Scene(root));
                    newStage.show();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                // Optionally show an alert - avoid importing Alert here to keep controller lightweight
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }
}
