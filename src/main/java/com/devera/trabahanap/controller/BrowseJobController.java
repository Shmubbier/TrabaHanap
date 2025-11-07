package com.devera.trabahanap.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;

import java.net.URL;
import java.util.ResourceBundle;

public class BrowseJobController extends Controller implements Initializable {

    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> recentCombo;
    @FXML private ComboBox<String> locationCombo;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        //  Category options
        categoryCombo.getItems().addAll(
                "All Categories",
                "Errands",
                "Tutoring",
                "Graphic Design",
                "Delivery",
                "Cleaning",
                "Photography",
                "Writing",
                "Tech Support"
        );

        //  Sort By options
        recentCombo.getItems().addAll(
                "Most Recent",
                "Price: Low to High",
                "Price: High to Low",
                "Highest Rated"
        );

        // Location options
        locationCombo.getItems().addAll(
                "All Locations",
                "Metro Manila",
                "Quezon City",
                "Makati",
                "BGC",
                "Pasig",
                "Pampanga"
        );

        // (Optional) set defaults
        categoryCombo.getSelectionModel().selectFirst();
        recentCombo.getSelectionModel().selectFirst();
        locationCombo.getSelectionModel().selectFirst();
    }
}
