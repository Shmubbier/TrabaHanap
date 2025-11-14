package com.devera.trabahanap.controller;

import com.devera.trabahanap.core.Job;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class JobDetailsController {

    // Left Column
    @FXML private Label jobTitleLabel;
    @FXML private Label jobLocationLabel;
    @FXML private Label jobTimestampLabel;
    @FXML private Label jobSalaryLabel;
    @FXML private Button categoryLabel;
    @FXML private Label jobDescriptionLabel;
    @FXML private Label jobExperienceLabel;
    @FXML private ImageView jobImageView;

    // Right Column
    @FXML private ImageView hostImageView;
    @FXML private Label hostNameLabel;

    @FXML private Button backButton;
    @FXML private Button applyButton;

    private HomeController homeController;

    public void setHomeController(HomeController hc) {
        this.homeController = hc;
    }

    /**
     * Populate the JobDetails UI with a Job object.
     */
    public void setJob(Job job) {
        if (job == null) return;

        // Left column
        jobTitleLabel.setText(job.getTitle() != null ? job.getTitle() : "(No Title)");
        jobLocationLabel.setText(job.getLocation() != null ? job.getLocation() : "");
        jobTimestampLabel.setText(String.valueOf(job.getTimestamp())); // convert long to String
        jobSalaryLabel.setText(job.getSalaryRange() != null ? job.getSalaryRange() : "");
        jobDescriptionLabel.setText(job.getDescription() != null ? job.getDescription() : "");
        jobExperienceLabel.setText(job.getExperienceLevel() != null ? job.getExperienceLevel() : "");
        categoryLabel.setText(job.getCategoryDisplay() != null ? job.getCategoryDisplay() : "Other");

        // Job image (if you have a mapping from imageKey)
        if (job.getImageKey() != null && !job.getImageKey().isBlank()) {
            // Example: load from local resource
            Image img = new Image("/images/" + job.getImageKey() + ".png", true);
            jobImageView.setImage(img);
        }

        // Right column: placeholders (since Job has no client info)
        hostNameLabel.setText("Client Name"); // replace if you add a client field
        hostImageView.setImage(new Image("/icons/default-user.png")); // default avatar

        // Back button action
        backButton.setOnAction(e -> {
            if (homeController != null) {
                homeController.loadPage("BrowseJobContent.fxml");
            }
        });

        // Apply button action (optional)
        applyButton.setOnAction(e -> {
            System.out.println("Applying for job: " + job.getTitle());
        });
    }
}
