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
public class Controller {

    /**
     * Replace the current scene's root with an FXML file.
     * @param stage the Stage to change; if null, tries to get from any owned node (override if needed)
     * @param fxmlPath resource path (e.g. /fxml/Home.fxml)
     * @param width scene width - if <= 0 will keep current width
     * @param height scene height - if <= 0 will keep current height
     * @throws IOException when FXMLLoader can't load the fxml
     */
    protected void navigate(Stage stage, String fxmlPath, double width, double height) throws IOException {
        if (stage == null) {
            // try to find any showing window and use its stage
            java.util.Optional<javafx.stage.Window> opt = javafx.stage.Window.getWindows().stream()
                    .filter(javafx.stage.Window::isShowing)
                    .findFirst();
            if (opt.isPresent() && opt.get() instanceof Stage) {
                stage = (Stage) opt.get();
            }
        }

        if (stage == null) {
            throw new IOException("No active Stage found to navigate to: " + fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        javafx.scene.Parent root = loader.load();

        Scene currentScene = stage.getScene();
        double newWidth = width > 0 ? width : (currentScene != null ? currentScene.getWidth() : -1);
        double newHeight = height > 0 ? height : (currentScene != null ? currentScene.getHeight() : -1);

        if (currentScene == null) {
            Scene scene = new Scene(root, newWidth > 0 ? newWidth : 800, newHeight > 0 ? newHeight : 600);
            // preserve stylesheet if available on application resources
            if (getClass().getResource("/css/app.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            }
            stage.setScene(scene);
        } else {
            // preserve scene and only replace the root so controllers and styles keep working
            currentScene.setRoot(root);
            if (newWidth > 0 && newHeight > 0) {
                stage.setWidth(newWidth);
                stage.setHeight(newHeight);
            }
        }
    }

    /**
     * Convenience overload when using current stage and default size.
     */
    protected void navigate(String fxmlPath) throws IOException {
        // Use the other navigate method, passing null stage to let it resolve the current showing window
        navigate(null, fxmlPath, -1, -1);
    }

    // Add shared helper methods for controllers here (e.g., showDialog, showError, common validation)
}