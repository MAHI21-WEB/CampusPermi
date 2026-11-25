package com.campus.permission.controller;

import com.campus.permission.exception.DatabaseException;
import com.campus.permission.exception.ValidationException;
import com.campus.permission.model.User;
import com.campus.permission.service.AuthService;
import com.campus.permission.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the CPMS login screen.
 */
public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        hideError();
    }

    @FXML
    private void handleLogin() {
        hideError();
        try {
            User user = authService.login(emailField.getText(), passwordField.getText());
            passwordField.clear();
            SceneNavigator.navigateToDashboard(user.getRole());
        } catch (ValidationException ex) {
            showError(ex.getMessage());
        } catch (DatabaseException ex) {
            LOGGER.log(Level.SEVERE, "Database error during login.", ex);
            showError("Unable to connect to the database. Please check configuration and MySQL status.");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unexpected error during login.", ex);
            showError("An unexpected error occurred. Please try again.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
