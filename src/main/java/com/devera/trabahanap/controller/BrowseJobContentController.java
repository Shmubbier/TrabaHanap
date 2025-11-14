package com.devera.trabahanap.controller;

import com.devera.trabahanap.core.Job;
import com.devera.trabahanap.service.JobService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for BrowseJob_Content.fxml.
 * Responsible for loading all job cards, filtering, and forwarding clicks to HomeController.
 */
public class BrowseJobContentController {

    public static BrowseJobContentController lastLoadedInstance;

    @FXML private VBox jobsVBox;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> recentCombo;
    @FXML private ComboBox<String> locationCombo;
    @FXML private TextField searchFieldBrowse;

    private final JobService jobService = new JobService();
    private HomeController homeController; // injected externally
    private List<Job> allJobs = new ArrayList<>();

    //--------------------------------------------------------------------------
    // Inject HomeController
    //--------------------------------------------------------------------------
    public void setHomeController(HomeController hc) {
        this.homeController = hc;
        lastLoadedInstance = this; // keep track of last loaded instance
    }

    //--------------------------------------------------------------------------
    // Initialization
    //--------------------------------------------------------------------------
    @FXML
    public void initialize() {
        lastLoadedInstance = this; // important for external applyFilters calls
        setupFilters();
        loadJobs();
    }

    //--------------------------------------------------------------------------
    // Setup combo box values and event handling
    //--------------------------------------------------------------------------
    private void setupFilters() {
        if (categoryCombo != null) {
            categoryCombo.getItems().addAll(
                    "All Categories",
                    "Graphics and Design",
                    "Music and Audio",
                    "Programming and Tech",
                    "Digital Marketing",
                    "Video and Animation",
                    "Writing and Translation",
                    "Business",
                    "Lifestyle",
                    "AI Services"
            );
            categoryCombo.getSelectionModel().select(0);
            categoryCombo.setOnAction(e -> applyFilters(null, null, null));
        }

        if (recentCombo != null) {
            recentCombo.getItems().addAll("Most Recent", "Oldest First");
            recentCombo.getSelectionModel().select(0);
            recentCombo.setOnAction(e -> applyFilters(null, null, null));
        }

        if (locationCombo != null) {
            locationCombo.getItems().addAll("All Location", "Luzon", "Visayas", "Mindanao", "Metro Manila");
            locationCombo.getSelectionModel().select(0);
            locationCombo.setOnAction(e -> applyFilters(null, null, null));
        }

        if (searchFieldBrowse != null) {
            searchFieldBrowse.textProperty().addListener((obs, oldV, newV) -> applyFilters(null, null, null));
        }
    }

    //--------------------------------------------------------------------------
    // Load jobs from Firestore using JobService
    //--------------------------------------------------------------------------
    private void loadJobs() {
        jobService.getAllJobs().whenComplete((list, err) -> {
            Platform.runLater(() -> {
                if (err != null) {
                    err.printStackTrace();
                    return;
                }
                allJobs = list != null ? list : new ArrayList<>();
                applyFilters(null, null, null);
            });
        });
    }

    //--------------------------------------------------------------------------
    // Apply filters (search + category + location + recent)
    //--------------------------------------------------------------------------
    public void applyFilters(String categoryParam, String locationParam, String recentParam) {
        if (allJobs == null) return;

        String search = searchFieldBrowse != null ? searchFieldBrowse.getText().trim().toLowerCase() : "";

        String category = categoryParam != null ? categoryParam :
                (categoryCombo != null ? categoryCombo.getValue() : "All Categories");
        String location = locationParam != null ? locationParam :
                (locationCombo != null ? locationCombo.getValue() : "All Location");
        String sortOption = recentParam != null ? recentParam :
                (recentCombo != null ? recentCombo.getValue() : "Most Recent");

        List<Job> filtered = allJobs.stream()
                .filter(j -> search.isEmpty() || j.getTitle().toLowerCase().contains(search))
                .filter(j -> "All Categories".equals(category) || category.equalsIgnoreCase(j.getCategory()))
                .filter(j -> "All Location".equals(location) || (j.getLocation() != null && j.getLocation().contains(location)))
                .toList();

        if ("Most Recent".equals(sortOption)) {
            filtered.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        } else {
            filtered.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        }

        renderJobCards(filtered);
    }

    //--------------------------------------------------------------------------
    // Render job cards into jobsVBox
    //--------------------------------------------------------------------------
    private void renderJobCards(List<Job> jobs) {
        if (jobsVBox == null) return;

        jobsVBox.getChildren().clear();

        for (Job job : jobs) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/JobCard.fxml"));
                Node card = loader.load();

                JobCardController controller = loader.getController();
                controller.setJob(job);
                controller.setOnCardClick(() -> {
                    if (homeController != null) homeController.openJobDetails(job);
                });

                jobsVBox.getChildren().add(card);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
