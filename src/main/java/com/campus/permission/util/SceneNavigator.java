package com.campus.permission.util;

import com.campus.permission.controller.RequestDetailsController;
import com.campus.permission.enums.UserRole;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralizes JavaFX scene navigation and stylesheet loading.
 */
public final class SceneNavigator {

    private static final Logger LOGGER = Logger.getLogger(SceneNavigator.class.getName());
    private static final String STYLESHEET = "/css/style.css";
    private static Stage primaryStage;

    private SceneNavigator() {
    }

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void navigateTo(String fxmlPath, String title) {
        loadScene(fxmlPath, title);
    }

    public static void openRequestDetails(int requestId) {
        FXMLLoader loader = loadScene("/fxml/request-details.fxml",
                "CPMS — Request #" + requestId + " Details");
        RequestDetailsController controller = loader.getController();
        controller.loadRequest(requestId);
    }

    public static void navigateToDashboard(UserRole role) {
        switch (role) {
            case CLUB_REP -> navigateTo("/fxml/representative-dashboard.fxml",
                    "CPMS — Club Representative Dashboard");
            case FACULTY_ADVISOR -> navigateTo("/fxml/faculty-dashboard.fxml",
                    "CPMS — Faculty Advisor Dashboard");
            case DOSA_REP -> navigateTo("/fxml/dosa-dashboard.fxml",
                    "CPMS — DOSA Representative Dashboard");
            case ASSOCIATE_DEAN -> navigateTo("/fxml/dean-dashboard.fxml",
                    "CPMS — Associate Dean Dashboard");
        }
    }

    private static FXMLLoader loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(SceneNavigator.class.getResource(fxmlPath),
                            "Missing FXML resource: " + fxmlPath)
            );
            Parent root = loader.load();
            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root, 1080, 700);
                scene.getStylesheets().add(
                        Objects.requireNonNull(SceneNavigator.class.getResource(STYLESHEET)).toExternalForm()
                );
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            primaryStage.setTitle(title);
            return loader;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to view: " + fxmlPath, ex);
            throw new IllegalStateException("Unable to load screen: " + fxmlPath, ex);
        }
    }
}
