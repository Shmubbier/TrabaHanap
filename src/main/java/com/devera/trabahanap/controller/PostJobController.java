package com.devera.trabahanap.controller;

import com.devera.trabahanap.core.Job;
import com.devera.trabahanap.service.JobService;
import com.devera.trabahanap.system.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.stream.Collectors;

public class PostJobController extends Controller {

    @FXML private TextField titleField; // maps to postJobTitleField
    @FXML private ComboBox<String> categoryComboBox; // maps to postCategoryCombo
    @FXML private TextArea descriptionArea; // maps to postJobDescriptionField
    @FXML private TextField locationField; // maps to postJobLocationField
    @FXML private Spinner<Double> budgetMinSpinner; // maps to postJobPriceMin
    @FXML private Spinner<Double> budgetMaxSpinner; // maps to postJobPriceMax
    @FXML private ComboBox<String> durationComboBox; // maps to postJobDurationCombo
    @FXML private TextField skillsField; // maps to postSkillsRequiredField
    @FXML private ComboBox<String> experienceLevelComboBox; // maps to postExperienceLevelCombo
    @FXML private TextArea specialInstructionsArea; // maps to postSpecialInstructionsField
    @FXML private Button postButton; // maps to postJobBtn
    @FXML private Button cancelButton; // maps to postDraftBtn

    private final JobService jobService = new JobService();

    @FXML
    public void initialize() {
        categoryComboBox.getItems().setAll(
                "Graphic Design",
                "Writing & Content",
                "Programming",
                "Video Editing",
                "Marketing",
                "Data Entry",
                "Translation",
                "Web Design",
                "Mobile Development",
                "Consulting",
                "Other"
        );
        categoryComboBox.getSelectionModel().selectFirst();

        experienceLevelComboBox.getItems().setAll("Entry", "Intermediate", "Expert");
        experienceLevelComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void onPostClicked() {
        String title = safeText(titleField);
        String location = safeText(locationField);
        String description = safeText(descriptionArea);
        String skillsInput = safeText(skillsField);
        String experienceLevel = experienceLevelComboBox.getValue();
        String categoryDisplay = categoryComboBox.getValue();

        Double budgetMin = budgetMinSpinner.getValue();
        Double budgetMax = budgetMaxSpinner.getValue();

        if (title.isBlank()) {
            showError("Job title is required.");
            return;
        }
        if (description.isBlank()) {
            showError("Job description is required.");
            return;
        }
        if (categoryDisplay == null || categoryDisplay.isBlank()) {
            showError("Please select a category.");
            return;
        }

        String userId = SessionManager.get().getLocalId().orElse(null);
        if (userId == null) {
            showError("You must be logged in to post a job.");
            return;
        }

        List<String> skillsList = new ArrayList<>();
        if (!skillsInput.isBlank()) {
            skillsList = Arrays.stream(skillsInput.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        String categoryKey = toCategoryKey(categoryDisplay);

        Job job = Job.createForPosting(
                title,
                SessionManager.get().getDisplayName().orElse(""),
                location,
                description,
                formatBudgetRange(budgetMin, budgetMax),
                userId,
                budgetMin,
                budgetMax,
                categoryDisplay,
                categoryKey,
                skillsList,
                experienceLevel
        );

        postButton.setDisable(true);
        jobService.addJob(job).whenComplete((docId, throwable) -> {
            Platform.runLater(() -> {
                postButton.setDisable(false);
                if (throwable != null) {
                    throwable.printStackTrace();
                    showError("Failed to post job: " + throwable.getMessage());
                    return;
                }

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Job Posted");
                success.setHeaderText("Your job has been posted successfully!");
                success.setContentText("Job ID: " + docId);
                success.showAndWait();

                try {
                    navigate("/fxml/Home.fxml");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    @FXML
    private void onCancelClicked() {
        try {
            navigate("/fxml/Home.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String safeText(TextInputControl c) {
        return c != null && c.getText() != null ? c.getText().trim() : "";
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Invalid Input");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private String formatBudgetRange(Double min, Double max) {
        if (min != null && max != null) {
            return "₱" + String.format(java.util.Locale.US, "%.2f", min) + " - ₱" + String.format(java.util.Locale.US, "%.2f", max);
        } else if (min != null) {
            return "₱" + String.format(java.util.Locale.US, "%.2f", min) + "+";
        } else if (max != null) {
            return "Up to ₱" + String.format(java.util.Locale.US, "%.2f", max);
        }
        return "Negotiable";
    }

    private String toCategoryKey(String name) {
        if (name == null) return "OTHER";
        switch (name) {
            case "Graphic Design": return "GRAPHIC_DESIGN";
            case "Writing & Content": return "WRITING";
            case "Programming": return "PROGRAMMING";
            case "Video Editing": return "VIDEO_EDITING";
            case "Marketing": return "MARKETING";
            case "Data Entry": return "DATA_ENTRY";
            case "Translation": return "TRANSLATION";
            case "Web Design": return "WEB_DESIGN";
            case "Mobile Development": return "MOBILE_DEV";
            case "Consulting": return "CONSULTING";
            default: return "OTHER";
        }
    }
}
