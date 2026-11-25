package com.campus.permission.controller;

import com.campus.permission.enums.UserRole;
import com.campus.permission.exception.DatabaseException;
import com.campus.permission.exception.UnauthorizedActionException;
import com.campus.permission.exception.ValidationException;
import com.campus.permission.model.PermissionRequest;
import com.campus.permission.model.PermissionType;
import com.campus.permission.model.User;
import com.campus.permission.service.AttachmentService;
import com.campus.permission.service.RequestService;
import com.campus.permission.util.SceneNavigator;
import com.campus.permission.util.SessionContext;
import com.campus.permission.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for creating and submitting permission requests.
 */
public class RequestController {

    private static final Logger LOGGER = Logger.getLogger(RequestController.class.getName());

    @FXML
    private ComboBox<PermissionType> categoryCombo;

    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private DatePicker eventDatePicker;

    @FXML
    private TextField startTimeField;

    @FXML
    private TextField endTimeField;

    @FXML
    private Label attachmentNameLabel;

    @FXML
    private Label errorLabel;

    private final RequestService requestService = new RequestService();
    private final AttachmentService attachmentService = new AttachmentService();
    private File selectedAttachment;

    @FXML
    public void initialize() {
        hideError();
        loadCategories();
    }

    private void loadCategories() {
        try {
            List<PermissionType> types = requestService.getActivePermissionTypes();
            categoryCombo.setItems(FXCollections.observableArrayList(types));
        } catch (DatabaseException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load permission categories.", ex);
            showError("Unable to load permission categories from the database.");
        }
    }

    @FXML
    private void handleChooseAttachment() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Supporting Document");
        File file = chooser.showOpenDialog(SceneNavigator.getPrimaryStage());
        if (file != null) {
            selectedAttachment = file;
            attachmentNameLabel.setText(file.getName());
        }
    }

    @FXML
    private void handleSaveDraft() {
        saveRequest(false);
    }

    @FXML
    private void handleSubmitRequest() {
        saveRequest(true);
    }

    @FXML
    private void handleCancel() {
        SceneNavigator.navigateToDashboard(UserRole.CLUB_REP);
    }

    private void saveRequest(boolean submitImmediately) {
        hideError();
        try {
            User currentUser = SessionContext.requireCurrentUser();
            LocalDate eventDate = eventDatePicker.getValue();
            LocalTime startTime = parseOptionalTime(startTimeField.getText(), "Start time");
            LocalTime endTime = parseOptionalTime(endTimeField.getText(), "End time");

            PermissionRequest saved = requestService.createRequest(
                    currentUser,
                    categoryCombo.getValue(),
                    titleField.getText(),
                    descriptionArea.getText(),
                    eventDate,
                    startTime,
                    endTime,
                    submitImmediately
            );

            if (selectedAttachment != null) {
                attachmentService.uploadAttachment(saved.getRequestId(), selectedAttachment);
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(submitImmediately ? "Request Submitted" : "Draft Saved");
            alert.setHeaderText(null);
            alert.setContentText(submitImmediately
                    ? "Request #" + saved.getRequestId() + " submitted for Faculty Advisor review."
                    : "Request #" + saved.getRequestId() + " saved as a draft.");
            alert.showAndWait();

            SceneNavigator.navigateToDashboard(UserRole.CLUB_REP);
        } catch (ValidationException | UnauthorizedActionException ex) {
            showError(ex.getMessage());
        } catch (DatabaseException ex) {
            LOGGER.log(Level.SEVERE, "Failed to save permission request.", ex);
            showError("Unable to save request due to a database error.");
        }
    }

    private LocalTime parseOptionalTime(String raw, String fieldName) {
        if (ValidationUtil.isBlank(raw)) {
            return null;
        }
        try {
            return LocalTime.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new ValidationException(fieldName + " must be in HH:mm format (e.g. 14:30).");
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
