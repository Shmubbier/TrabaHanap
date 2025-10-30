module com.devera.trabahanap {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;

    // Third-party libraries (jackson, controlsfx, firebase-admin, google-auth, etc.)
    // are kept on the classpath (not declared as module requires here) because many of
    // them are non-modular or have incompatible automatic module names.
    //
    // If you later modularize or verify automatic module names for a library, add a
    // `requires <module-name>;` line here.

    // Allow FXMLLoader reflective access to our application packages:
    opens com.devera.trabahanap to javafx.fxml;
    opens com.devera.trabahanap.controller to javafx.fxml;
    opens com.devera.trabahanap.core to javafx.fxml;
    opens com.devera.trabahanap.util to javafx.fxml;

    // Export application API packages if other modules need them (not strictly necessary now)
    exports com.devera.trabahanap;
    exports com.devera.trabahanap.controller;
    exports com.devera.trabahanap.core;
    exports com.devera.trabahanap.util;
}