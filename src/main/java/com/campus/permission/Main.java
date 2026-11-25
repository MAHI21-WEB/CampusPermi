package com.campus.permission;

import com.campus.permission.util.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.logging.Logger;

/**
 * Entry point for the Campus Permission Management System (CPMS) JavaFX desktop application.
 */
public class Main extends Application {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final String APP_TITLE = "Campus Permission Management System (CPMS) — Login";

    @Override
    public void start(Stage primaryStage) {
        SceneNavigator.setPrimaryStage(primaryStage);
        primaryStage.setMinWidth(920);
        primaryStage.setMinHeight(600);
        SceneNavigator.navigateTo("/fxml/login.fxml", APP_TITLE);
        primaryStage.show();
        LOGGER.info("CPMS JavaFX application started successfully.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
