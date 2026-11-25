package com.campus.permission.controller;

import com.campus.permission.enums.Decision;
import com.campus.permission.exception.DatabaseException;
import com.campus.permission.exception.ResourceNotFoundException;
import com.campus.permission.exception.UnauthorizedActionException;
import com.campus.permission.exception.ValidationException;
import com.campus.permission.model.Approval;
import com.campus.permission.model.Attachment;
import com.campus.permission.model.PermissionRequest;
import com.campus.permission.model.User;
import com.campus.permission.service.ApprovalService;
import com.campus.permission.service.AttachmentService;
import com.campus.permission.service.DashboardService;
import com.campus.permission.service.RequestService;
import com.campus.permission.util.SceneNavigator;
import com.campus.permission.util.SessionContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reusable controller for viewing full request details, approval timeline, and submitting decisions.
 */
public class RequestDetailsController {

    private static final Logger LOGGER = Logger.getLogger(RequestDetailsController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @FXML
    private Label requestHeaderLabel;

    @FXML
    private Label statusBadgeLabel;

    @FXML
    private Label societyLabel;

    @FXML
    private Label submittedByLabel;

    @FXML
    private Label categoryLabel;

    @FXML
    private Label scheduleLabel;

    @FXML
    private Label submittedAtLabel;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TableView<Approval> historyTable;

    @FXML
    private TableColumn<Approval, String> levelColumn;

    @FXML
    private TableColumn<Approval, String> approverColumn;

    @FXML
    private TableColumn<Approval, String> decisionColumn;

    @FXML
    private TableColumn<Approval, String> remarksColumn;

    @FXML
    private TableColumn<Approval, String> decisionDateColumn;

    @FXML
    private ListView<Attachment> attachmentsListView;

    @FXML
    private Button openAttachmentButton;

    @FXML
    private TextArea timelineArea;

    @FXML
    private VBox decisionPanel;

    @FXML
    private TextArea remarksArea;

    @FXML
    private Label decisionErrorLabel;

    private final RequestService requestService = new RequestService();
    private final ApprovalService approvalService = new ApprovalService();
    private final AttachmentService attachmentService = new AttachmentService();
    private final DashboardService dashboardService = new DashboardService();

    private int currentRequestId;

    @FXML
    public void initialize() {
        levelColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getApprovalLevel().getDisplayName()));
        approverColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getApproverName()));
        decisionColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDecision().getDisplayName()));
        remarksColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getRemarks() != null ? cell.getValue().getRemarks() : ""));
        decisionDateColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDecisionDate() != null
                        ? cell.getValue().getDecisionDate().format(DATE_TIME_FORMAT)
                        : "—"));

        if (attachmentsListView != null && openAttachmentButton != null) {
            attachmentsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) ->
                    openAttachmentButton.setDisable(newSel == null));
        }

        hideDecisionError();
    }

    public void loadRequest(int requestId) {
        this.currentRequestId = requestId;
        try {
            User currentUser = SessionContext.requireCurrentUser();
            PermissionRequest request = requestService.getRequestForUser(currentUser, requestId);

            requestHeaderLabel.setText("Request #" + request.getRequestId() + " — " + request.getTitle());
            statusBadgeLabel.setText(request.getStatus().getDisplayName());
            societyLabel.setText(request.getSocietyName() != null ? request.getSocietyName() : "—");
            submittedByLabel.setText(request.getSubmittedByName() != null ? request.getSubmittedByName() : "—");
            categoryLabel.setText(request.getPermissionType() != null ? request.getPermissionType().getTypeName() : "—");
            scheduleLabel.setText(formatSchedule(request));
            submittedAtLabel.setText(request.getCreatedAt() != null
                    ? request.getCreatedAt().format(DATE_TIME_FORMAT)
                    : "—");
            descriptionArea.setText(request.getDescription());

            List<Approval> history = approvalService.getApprovalHistory(requestId);
            historyTable.setItems(FXCollections.observableArrayList(history));

            if (attachmentsListView != null) {
                List<Attachment> attachments = attachmentService.getAttachmentsForRequest(requestId);
                attachmentsListView.setItems(FXCollections.observableArrayList(attachments));
            }
            if (timelineArea != null) {
                LinkedList<String> timeline = dashboardService.buildActivityTimeline(request, history);
                timelineArea.setText(dashboardService.formatTimelineSummary(timeline));
            }

            boolean canDecide = approvalService.canUserActOnRequest(currentUser, request);
            decisionPanel.setVisible(canDecide);
            decisionPanel.setManaged(canDecide);
        } catch (ResourceNotFoundException | UnauthorizedActionException | DatabaseException ex) {
            LOGGER.log(Level.WARNING, "Failed to load request details for id=" + requestId, ex);
            showAlert(Alert.AlertType.ERROR, "Unable to Open Request", ex.getMessage());
            handleBack();
        }
    }

    @FXML
    private void handleOpenAttachment() {
        Attachment selected = attachmentsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            File file = attachmentService.resolveAttachmentFile(selected);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Attachment Location", file.getAbsolutePath());
            }
        } catch (ValidationException | IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Unable to Open Attachment", ex.getMessage());
        }
    }

    @FXML
    private void handleApprove() {
        submitDecision(Decision.APPROVED);
    }

    @FXML
    private void handleReject() {
        submitDecision(Decision.REJECTED);
    }

    @FXML
    private void handleBack() {
        User currentUser = SessionContext.getCurrentUser();
        if (currentUser != null) {
            SceneNavigator.navigateToDashboard(currentUser.getRole());
        } else {
            SceneNavigator.navigateTo("/fxml/login.fxml",
                    "Campus Permission Management System (CPMS) — Login");
        }
    }

    private void submitDecision(Decision decision) {
        hideDecisionError();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm " + decision.getDisplayName());
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to mark Request #" + currentRequestId
                + " as " + decision.getDisplayName() + "?");
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }

        try {
            User currentUser = SessionContext.requireCurrentUser();
            approvalService.processDecision(currentUser, currentRequestId, decision, remarksArea.getText());
            showAlert(Alert.AlertType.INFORMATION, "Decision Recorded",
                    "Request #" + currentRequestId + " has been " + decision.getDisplayName().toLowerCase() + ".");
            handleBack();
        } catch (ValidationException | UnauthorizedActionException | ResourceNotFoundException ex) {
            showDecisionError(ex.getMessage());
        } catch (DatabaseException ex) {
            LOGGER.log(Level.SEVERE, "Database error while recording decision.", ex);
            showDecisionError("Unable to record decision due to a database error. No changes were saved.");
        }
    }

    private String formatSchedule(PermissionRequest request) {
        if (request.getEventDate() == null) {
            return "Not specified";
        }
        StringBuilder sb = new StringBuilder(request.getEventDate().format(DATE_FORMAT));
        if (request.getStartTime() != null) {
            sb.append(" | ").append(request.getStartTime());
            if (request.getEndTime() != null) {
                sb.append(" – ").append(request.getEndTime());
            }
        }
        return sb.toString();
    }

    private void showDecisionError(String message) {
        decisionErrorLabel.setText(message);
        decisionErrorLabel.setVisible(true);
        decisionErrorLabel.setManaged(true);
    }

    private void hideDecisionError() {
        decisionErrorLabel.setText("");
        decisionErrorLabel.setVisible(false);
        decisionErrorLabel.setManaged(false);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
