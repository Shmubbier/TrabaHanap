package com.devera.trabahanap.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class HomeContentController {

    private HomeController homeController;

    // Trending Jobs HBox (but HomeController fills it)
    @FXML private HBox jobsHBox;

    // Search fields
    @FXML private TextField searchFieldMain;
    @FXML private TextField searchFieldLocation;
    @FXML private Button searchBtn;

    // Popular tags
    @FXML private Button poptagGraphicDesign;
    @FXML private Button poptagTutoring;
    @FXML private Button poptagDelivery;
    @FXML private Button poptagCleaning;

    // Category buttons
    @FXML private Button categoryErrandsBtn;
    @FXML private Button categoryTutoringBtn;
    @FXML private Button categoryGraphicDesignBtn;
    @FXML private Button categoryDeliveryBtn;
    @FXML private Button categoryCleaningBtn;
    @FXML private Button categoryPhotographyBtn;
    @FXML private Button categoryWritingBtn;

    // View All
    @FXML private Button viewAllCategoryBtn;
    @FXML private Button viewAllJobsBtn;

    /**
     * Injected by HomeController after loading this FXML.
     */
    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    @FXML
    public void initialize() {
        setupSearch();
        setupPopularTagButtons();
        setupCategoryButtons();
        setupViewAllButtons();
    }

    private void setupSearch() {
        if (searchBtn != null) {
            searchBtn.setOnAction(e -> {
                String query = searchFieldMain.getText();
                String location = searchFieldLocation.getText();

                navigateToBrowsePage(query, location, null);
            });
        }
    }

    private void setupPopularTagButtons() {
        if (poptagGraphicDesign != null)
            poptagGraphicDesign.setOnAction(e -> navigateToBrowsePage("Graphic Design", null, "Graphic Design"));

        if (poptagTutoring != null)
            poptagTutoring.setOnAction(e -> navigateToBrowsePage("Tutoring", null, "Tutoring"));

        if (poptagDelivery != null)
            poptagDelivery.setOnAction(e -> navigateToBrowsePage("Delivery", null, "Delivery"));

        if (poptagCleaning != null)
            poptagCleaning.setOnAction(e -> navigateToBrowsePage("Cleaning", null, "Cleaning"));
    }

    private void setupCategoryButtons() {

        if (categoryErrandsBtn != null)
            categoryErrandsBtn.setOnAction(e -> navigateToBrowsePage(null, null, "Errands"));

        if (categoryTutoringBtn != null)
            categoryTutoringBtn.setOnAction(e -> navigateToBrowsePage(null, null, "Tutoring"));

        if (categoryGraphicDesignBtn != null)
            categoryGraphicDesignBtn.setOnAction(e -> navigateToBrowsePage(null, null, "Graphic Design"));

        if (categoryDeliveryBtn != null)
            categoryDeliveryBtn.setOnAction(e -> navigateToBrowsePage(null, null, "Delivery"));

        if (categoryCleaningBtn != null)
            categoryCleaningBtn.setOnAction(e -> navigateToBrowsePage(null, null, "Cleaning"));

        if (categoryPhotographyBtn != null)
            categoryPhotographyBtn.setOnAction(e -> navigateToBrowsePage(null, null, "Photography"));

        if (categoryWritingBtn != null)
            categoryWritingBtn.setOnAction(e -> navigateToBrowsePage(null, null, "Writing"));
    }

    private void setupViewAllButtons() {
        if (viewAllCategoryBtn != null)
            viewAllCategoryBtn.setOnAction(e -> navigateToBrowsePage(null, null, null));

        if (viewAllJobsBtn != null)
            viewAllJobsBtn.setOnAction(e -> navigateToBrowsePage(null, null, null));
    }


    private void navigateToBrowsePage(String query, String location, String category) {
        if (homeController != null) {
            try {
                homeController.loadPage("BrowseJob_content.fxml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // After page loads, controller will be injected
        // so we defer category/search passing to BrowseJobContentController
        BrowseJobContentController controller = BrowseJobContentController.lastLoadedInstance;

        if (controller != null) {
            controller.applyFilters(); // no arguments
        }
    }
}
