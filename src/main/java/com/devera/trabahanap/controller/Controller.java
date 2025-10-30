package com.devera.trabahanap.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * BaseController: reusable navigation & common utilities for controllers.
 * Concrete controllers should extend this class to get navigation helpers.
 */
public abstract class BaseController {

    /**
     * Replace the current scene's root with an FXML file.
     * @param stage the Stage to change; if null, tries to get from any owned node (override if needed)
     * @param fxmlPath resource path (e.g. /fxml/Home.fxml)
     * @param width scene width - if <= 0 will keep current width
     * @param height scene height - if <= 0 will keep current height
     * @throws IOException when FXMLLoader can't load the fxml
     */
    protected void navigate(Stage stage, String fxmlPath, double width, double height) throws IOException {
        Objects.requireNonNull(fxmlPath);
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        if (stage == null) {
            // fallback: create a new stage
            stage = new Stage();
        }
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, Math.max(800, (width > 0 ? width : 800)), Math.max(600, (height > 0 ? height : 600)));
            stage.setScene(scene);
        } else {
            if (width > 0 && height > 0) {
                scene.setRoot(root);
                stage.setWidth(width);
                stage.setHeight(height);
            } else {
                scene.setRoot(root);
            }
        }
        stage.show();
    }

    /**
     * Convenience overload when using current stage and default size.
     */
    protected void navigate(String fxmlPath) throws IOException {
        navigate(null, fxmlPath, -1, -1);
    }

    // Add shared helper methods for controllers here (e.g., showDialog, showError, common validation)
}