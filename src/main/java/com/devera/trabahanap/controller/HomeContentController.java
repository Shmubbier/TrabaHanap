package com.devera.trabahanap.controller;

import com.devera.trabahanap.core.Job;
import com.devera.trabahanap.service.JobService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Home_Content.fxml.
 * Responsible for loading jobs into the homepage trending list (jobsHBox).
 */
public class HomeContentController {

    public static HomeContentController lastLoadedInstance;

    @FXML private HBox jobsHBox; // trending jobs on homepage

    private final JobService jobService = new JobService();
    private HomeController homeController; // injected externally
    private List<Job> allJobs = new ArrayList<>();

    //--------------------------------------------------------------------------
    // Inject HomeController
    //--------------------------------------------------------------------------
    public void setHomeController(HomeController hc) {
        this.homeController = hc;
        lastLoadedInstance = this;
    }

    //--------------------------------------------------------------------------
    // Initialization
    //--------------------------------------------------------------------------
    @FXML
    public void initialize() {
        lastLoadedInstance = this;
        loadJobs();
    }

    //--------------------------------------------------------------------------
    // Load jobs from Firestore and render into jobsHBox
    //--------------------------------------------------------------------------
    private void loadJobs() {
        jobService.getAllJobs().whenComplete((jobs, err) -> {
            Platform.runLater(() -> {
                if (err != null) {
                    err.printStackTrace();
                    return;
                }

                allJobs = jobs != null ? jobs : new ArrayList<>();
                renderTrendingJobs();
            });
        });
    }

    //--------------------------------------------------------------------------
    // Render trending jobs (up to 6) into jobsHBox
    //--------------------------------------------------------------------------
    private void renderTrendingJobs() {
        if (jobsHBox == null) return;

        jobsHBox.getChildren().clear();

        int max = Math.min(6, allJobs.size());

        for (int i = 0; i < max; i++) {
            Job job = allJobs.get(i);
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/JobCard.fxml"));
                Node card = loader.load();

                Object controller = loader.getController();
                if (controller instanceof JobCardController jcc) {
                    jcc.setJob(job);
                    jcc.setOnCardClick(() -> {
                        if (homeController != null) homeController.openJobDetails(job);
                    });
                }

                jobsHBox.getChildren().add(card);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //--------------------------------------------------------------------------
    // Public method for external refresh (like after posting a job)
    //--------------------------------------------------------------------------
    public void refreshJobs() {
        loadJobs();
    }
}
