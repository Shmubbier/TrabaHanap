package com.devera.trabahanap.controller;

import javafx.fxml.FXML;

public class SidebarController {

    private HomeController homeController;

    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    @FXML
    private void onHomeClick() {
        homeController.loadPage("Home_Content.fxml");
    }

    @FXML
    private void onBrowseClick() {
        homeController.loadPage("JobDetails.fxml");
    }

    @FXML
    private void onPostClick() {
        homeController.loadPage("PostJob.fxml");
    }

    @FXML
    private void onMessagesClick() {
        // TODO later
    }

    @FXML
    private void onProfileClick() {
        // TODO later
    }

    @FXML
    private void onSettingsClick() {
        // TODO later
    }

    @FXML
    private void onLogoutClick() {
        // TODO: call SessionManager.logout()
        System.out.println("Logging out...");
    }
}
