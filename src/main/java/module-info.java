module com.devera.trabahanap {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;

    // Firebase and Google
    requires firebase.admin;
    requires com.google.gson;
    // Allow reading unnamed modules
    requires static java.sql;
    requires com.google.auth;
    requires com.google.auth.oauth2;


    opens com.devera.trabahanap to javafx.fxml;
    opens com.devera.trabahanap.service;
    opens com.devera.trabahanap.system to com.google.auth, com.google.auth.oauth2, firebase.admin;

    opens com.devera.trabahanap.controller to javafx.fxml;
    exports com.devera.trabahanap.controller;

    exports com.devera.trabahanap;
    exports com.devera.trabahanap.system;
    exports com.devera.trabahanap.service;
}
