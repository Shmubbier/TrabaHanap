module com.devera.trabahanap {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires org.controlsfx.controls;
    requires com.fasterxml.jackson.databind;
    requires com.devera.trabahanap;

    // firebase-admin and google auth are automatic modules on classpath; no explicit requires here.
    // Add opens/exports for your app packages so FXMLLoader can access controllers.
    opens com.devera.trabahanap to javafx.fxml;
    exports com.devera.trabahanap;
    exports com.devera.trabahanap.controller;
    opens com.devera.trabahanap.controller to javafx.fxml;

    // Expose new package root for com.trabahanap packages (app code uses com.trabahanap)
    opens com.trabahanap to javafx.fxml;
    exports com.trabahanap;
    exports com.trabahanap.controller;
    opens com.trabahanap.controller to javafx.fxml;
}