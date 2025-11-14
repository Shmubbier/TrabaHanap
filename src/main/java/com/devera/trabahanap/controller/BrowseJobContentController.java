package com.devera.trabahanap.controller;

import com.devera.trabahanap.core.Job;
import com.devera.trabahanap.service.JobService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * Controller for BrowseJob_Content.fxml
 * Dynamically loads JobCard.fxml for each job and appends to jobsVBox.
 */
public class BrowseJobContentController extends Controller {

    @FXML
    private VBox jobsVBox;

    @FXML
    private ScrollPane scrollPane;

    private final JobService jobService = new JobService();

    @FXML
    public void initialize() {
        if (jobsVBox != null) {
            jobsVBox.getChildren().clear();
        }
        loadJobs();
    }

    private void loadJobs() {
        jobService.getAllJobs().whenComplete((jobs, err) -> {
            Platform.runLater(() -> {
                if (err != null) {
                    err.printStackTrace();
                    return;
                }
                renderJobs(jobs);
            });
        });
    }

    private void renderJobs(List<Job> jobs) {
        if (jobs == null || jobs.isEmpty() || jobsVBox == null) return;

        for (Job job : jobs) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/JobCard.fxml"));
                Node card = loader.load();

                Object controller = loader.getController();
                try {
                    controller.getClass().getMethod("setJob", Job.class).invoke(controller, job);
                } catch (ReflectiveOperationException ignored) {}

                card.setOnMouseClicked(ev -> openDetails(job));

                jobsVBox.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void openDetails(Job job) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/JobDetails.fxml"));
                Parent root = loader.load();

                Object controller = loader.getController();
                try {
                    controller.getClass().getMethod("setJob", Job.class).invoke(controller, job);
                } catch (ReflectiveOperationException ignored) {}

                Stage stage = (Stage) (jobsVBox != null ? jobsVBox.getScene().getWindow() : null);
                if (stage != null) {
                    Scene scene = stage.getScene();
                    if (scene == null) {
                        stage.setScene(new Scene(root));
                    } else {
                        scene.setRoot(root);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
