package com.devera.trabahanap.controller;

import com.devera.trabahanap.core.Job;
import com.devera.trabahanap.util.CategoryImageMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * Controller for JobDetails.fxml
 *
 * Responsibilities:
 *  - Expose public void setJob(Job) so callers can pass the selected Job.
 *  - Bind job data to the UI controls on the JavaFX Application Thread.
 *  - Provide a back navigation handler to return to the previous view.
 */
public class JobDetailsController extends Controller {

    @FXML private Label titleLabel;
    @FXML private Label companyLabel;
    @FXML private Label locationLabel;
    @FXML private Label postedDateLabel;
    @FXML private TextArea descriptionArea;
    @FXML private Label salaryLabel;
    @FXML private Button backButton;
    @FXML private Button detailsCategoryBtn;
    @FXML private ImageView detailsJobImageView;

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
     * Populate the UI with the passed Job object.
     * Safe to call off-FX thread: updates UI using Platform.runLater.
     */
    public void setJob(Job job) {
        this.job = job;
        if (job == null) return;

        Platform.runLater(() -> {
            titleLabel.setText(nonNullOrPlaceholder(job.getTitle(), "(No title)"));
            companyLabel.setText(nonNullOrPlaceholder(job.getCompanyName(), ""));
            locationLabel.setText(nonNullOrPlaceholder(job.getLocation(), ""));
            salaryLabel.setText(nonNullOrPlaceholder(job.getSalaryRange(), computeSalary(job)));
            descriptionArea.setText(nonNullOrPlaceholder(job.getDescription(), ""));
            postedDateLabel.setText(formatTimestamp(job.getTimestamp()));

            if (detailsCategoryBtn != null)
                detailsCategoryBtn.setText(job.getCategoryDisplay() != null ? job.getCategoryDisplay() : "Other");

            // Load local category/job image
            if (detailsJobImageView != null) {
                String key = job.getImageKey() != null ? job.getImageKey() : "OTHER";
                detailsJobImageView.setImage(CategoryImageMapper.getImage(key));
            }
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

    private String computeSalary(Job job) {
        Double min = job.getBudgetMin();
        Double max = job.getBudgetMax();
        if (min == null && max == null) return "N/A";
        if (min != null && max != null) return "₱" + min.intValue() + " – ₱" + max.intValue();
        if (min != null) return "₱" + min.intValue();
        return "₱" + max.intValue();
    }

    @FXML
    private void onBackClicked() {
        // Navigate back to Home or close window if navigation fails
        try {
            navigate("/fxml/Home.fxml");
        } catch (Exception e) {
            try {
                Stage s = (Stage) (backButton != null ? backButton.getScene().getWindow() : null);
                if (s != null) s.close();
            } catch (Exception ignored) {}
        }
    }
}
