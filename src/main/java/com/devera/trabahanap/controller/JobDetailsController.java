// Language: java
package com.devera.trabahanap.controller;

import com.devera.trabahanap.core.Job;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Controller for JobDetails.fxml
 *
 * Responsibilities:
 *  - Expose public void setJob(Job) so callers can pass the selected Job.
 *  - Bind job data to the UI controls on the JavaFX Application Thread.
 *  - Provide a back navigation handler to return to the Home view using the base Controller.navigate helper.
 */
public class JobDetailsController extends Controller {

    @FXML
    private Label titleLabel;

    @FXML
    private Label companyLabel;

    @FXML
    private Label locationLabel;

    @FXML
    private Label postedDateLabel;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Label salaryLabel;

    @FXML
    private Button backButton;

    // Keep a reference to the current job
    private Job job;

    // Date formatter for posted date
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault());

    @FXML
    public void initialize() {
        // Make description read-only and wrap text
        if (descriptionArea != null) {
            descriptionArea.setWrapText(true);
            descriptionArea.setEditable(false);
        }
    }

    /**
     * Called by other controllers (HomeController or JobCardController) via reflection.
     * Populate the UI with the passed Job object's data.
     *
     * Must be safe to call off-FX thread: uses Platform.runLater to update controls.
     */
    public void setJob(Job job) {
        this.job = job;
        if (job == null) return;

        Platform.runLater(() -> {
            if (titleLabel != null) titleLabel.setText(nonNullOrPlaceholder(job.getTitle(), "(No title)"));
            if (companyLabel != null) companyLabel.setText(nonNullOrPlaceholder(job.getCompanyName(), ""));
            if (locationLabel != null) locationLabel.setText(nonNullOrPlaceholder(job.getLocation(), ""));
            if (salaryLabel != null) salaryLabel.setText(nonNullOrPlaceholder(job.getSalaryRange(), ""));
            if (descriptionArea != null) descriptionArea.setText(nonNullOrPlaceholder(job.getDescription(), ""));
            if (postedDateLabel != null) postedDateLabel.setText(formatTimestamp(job.getTimestamp()));
        });
    }

    private String nonNullOrPlaceholder(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private String formatTimestamp(long epochMillis) {
        if (epochMillis <= 0) return "";
        try {
            LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
            return dateFormatter.format(ldt);
        } catch (Exception ex) {
            return "";
        }
    }

    @FXML
    private void onBackClicked() {
        // Navigate back to Home.fxml preserving window and styles. Use navigate helper.
        try {
            navigate("/fxml/Home.fxml");
        } catch (Exception e) {
            // If navigation fails, fallback to closing the window if it's a separate Stage.
            try {
                Stage s = (Stage) (backButton != null ? backButton.getScene().getWindow() : null);
                if (s != null) s.close();
            } catch (Exception ignored) {
            }
        }
    }
}
