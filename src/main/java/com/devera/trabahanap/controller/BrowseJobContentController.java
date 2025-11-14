package com.devera.trabahanap.controller;

import com.devera.trabahanap.core.Job;
import com.devera.trabahanap.service.JobService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

/**
 * Controller for BrowseJobContent.fxml
 */
public class BrowseJobContentController extends Controller {

    @FXML private VBox jobsVBox;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> recentCombo;
    @FXML private ComboBox<String> locationCombo;
    @FXML private Button moreFiltersBtn;
    @FXML private TextField searchFieldBrowse;

    private JobService jobService = new JobService();
    private HomeController homeController; // injected from HomeController

    public void setHomeController(HomeController hc) {
        this.homeController = hc;
    }

    @FXML
    public void initialize() {
        loadJobs();
    }

    private void loadJobs() {
        // Clear previous cards
        if (jobsVBox != null) Platform.runLater(jobsVBox.getChildren()::clear);

        jobService.getAllJobs().whenComplete((jobs, thr) -> {
            Platform.runLater(() -> {
                if (thr != null) {
                    System.err.println("Error fetching jobs: " + thr.getMessage());
                    return;
                }
                if (jobs == null || jobs.isEmpty() || jobsVBox == null) return;

                for (Job job : jobs) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/JobCard.fxml"));
                        Node card = loader.load();
                        Object controller = loader.getController();

                        if (controller instanceof JobCardController jobCardController) {
                            jobCardController.setJob(job);

                            // Click handler to load JobDetails in borderPane center
                            jobCardController.setOnCardClick(() -> openJobDetails(job));
                        }

                        jobsVBox.getChildren().add(card);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }

    private void openJobDetails(Job job) {
        if (homeController == null || job == null) return;

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/JobDetails.fxml"));
                Node root = loader.load();
                Object controller = loader.getController();

                if (controller instanceof JobDetailsController jobDetailsController) {
                    jobDetailsController.setJob(job);

                    // Inject homeController if needed
                    jobDetailsController.setHomeController(homeController);
                }

                // Set JobDetails FXML into center of homeController's borderPane
                BorderPane bp = homeController.getBorderPane();
                if (bp != null) bp.setCenter(root);

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
