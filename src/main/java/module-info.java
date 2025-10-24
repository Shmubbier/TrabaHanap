module com.devera.trabahanap {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires org.controlsfx.controls;
    requires com.fasterxml.jackson.databind;

    opens com.devera.trabahanap to javafx.fxml;
    exports com.devera.trabahanap;
    exports com.devera.trabahanap.controller;
    opens com.devera.trabahanap.controller to javafx.fxml;
}