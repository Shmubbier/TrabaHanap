package com.devera.trabahanap;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/fxml/Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 720);
        if (MainApp.class.getResource("/css/app.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        }
        stage.setTitle("TrabaHanap");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}