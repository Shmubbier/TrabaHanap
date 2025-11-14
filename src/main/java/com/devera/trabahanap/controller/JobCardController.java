package com.devera.trabahanap.controller;

import com.devera.trabahanap.core.Job;
import com.devera.trabahanap.util.CategoryImageMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

/**
 * Controller for JobCard.fxml
 */
public class JobCardController extends Controller {

    @FXML private Button cardButton;
    @FXML private Label titleLabel;
    @FXML private Label companyLabel;
    @FXML private Label locationLabel;
    @FXML private Label salaryLabel;
    @FXML private Label shortDescLabel;
    @FXML private Button categoryTagButton;
    @FXML private ImageView jobImageView; // newly added

    private Job job;

    // Delegate click handler injected by BrowseJobContentController
    private Runnable onClick;

    public void setOnCardClick(Runnable r) {
        this.onClick = r;
    }

    @FXML
    private void onCardHover() {
        if (cardButton != null && !cardButton.getStyleClass().contains("card-hover")) {
            cardButton.getStyleClass().add("card-hover");
        }
    }

    @FXML
    private void onCardExit() {
        if (cardButton != null) cardButton.getStyleClass().remove("card-hover");
    }

    @FXML
    private void onCardClicked() {
        if (onClick != null) onClick.run();
    }

    public void setJob(Job job) {
        this.job = job;
        if (job == null) return;

        Platform.runLater(() -> {
            titleLabel.setText(job.getTitle() != null ? job.getTitle() : "(No title)");
            companyLabel.setText(job.getCompanyName() != null ? job.getCompanyName() : "");
            locationLabel.setText(job.getLocation() != null ? job.getLocation() : "");
            salaryLabel.setText(job.getSalaryRange() != null ? job.getSalaryRange() : computeSalary(job));

            String desc = job.getDescription() != null ? job.getDescription() : "";
            shortDescLabel.setText(desc.length() > 120 ? desc.substring(0, 117) + "..." : desc);

            if (categoryTagButton != null) {
                categoryTagButton.setText(job.getCategoryDisplay() != null ? job.getCategoryDisplay() : "Other");
            }

            // Load local image based on imageKey / category key
            if (jobImageView != null) {
                String key = job.getImageKey() != null ? job.getImageKey() : "OTHER";
                jobImageView.setImage(CategoryImageMapper.getImage(key));
            }
        });
    }

    private String computeSalary(Job job) {
        Double min = job.getBudgetMin();
        Double max = job.getBudgetMax();
        if (min == null && max == null) return "N/A";
        if (min != null && max != null) return "₱" + min.intValue() + " – ₱" + max.intValue();
        if (min != null) return "₱" + min.intValue();
        return "₱" + max.intValue();
    }
}
