package com.devera.trabahanap.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Controller for JobCard.fxml — handles hover effects and other card interactions.
 */
public class JobCardController extends Controller {

    @FXML
    private Button cardButton;

    @FXML
    private void onCardHover() {
        if (cardButton != null) {
            cardButton.setStyle("-fx-background-color: transparent; -fx-border-color: #555; -fx-border-width: 1;");
        }
    }

    @FXML
    private void onCardExit() {
        if (cardButton != null) {
            cardButton.setStyle("-fx-background-color: transparent; -fx-border-color: #999; -fx-border-width: 1;");
        }
    }
}
