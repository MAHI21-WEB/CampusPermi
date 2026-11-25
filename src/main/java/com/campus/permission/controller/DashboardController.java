package com.campus.permission.controller;

import com.campus.permission.enums.RequestStatus;
import com.campus.permission.enums.UserRole;
import com.campus.permission.exception.DatabaseException;
import com.campus.permission.exception.UnauthorizedActionException;
import com.campus.permission.exception.ValidationException;
import com.campus.permission.model.PermissionRequest;
import com.campus.permission.model.User;
import com.campus.permission.service.AuthService;
import com.campus.permission.service.BackupService;
import com.campus.permission.service.DashboardService;
import com.campus.permission.service.RequestService;
import com.campus.permission.util.SceneNavigator;
import com.campus.permission.util.SessionContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

import java.io.File;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the role dashboards (currently powering the Representative Dashboard and shared header).
 */
public class DashboardController {

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @FXML
    private Label userNameLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label totalCountLabel;

    @FXML
    private Label pendingCountLabel;

    @FXML
    private Label approvedCountLabel;

    @FXML
    private Label rejectedCountLabel;

    @FXML
    private ComboBox<String> statusFilterCombo;

    @FXML
    private Button submitDraftButton;

    @FXML
    private Button viewDetailsButton;

    @FXML
    private TableView<PermissionRequest> requestsTable;

    @FXML
    private TableColumn<PermissionRequest, String> idColumn;

    @FXML
    private TableColumn<PermissionRequest, String> titleColumn;

    @FXML
    private TableColumn<PermissionRequest, String> categoryColumn;

    @FXML
    private TableColumn<PermissionRequest, String> eventDateColumn;

    @FXML
    private TableColumn<PermissionRequest, String> statusColumn;

    @FXML
    private TableColumn<PermissionRequest, String> createdColumn;

    private final AuthService authService = new AuthService();
    private final RequestService requestService = new RequestService();
    private final DashboardService dashboardService = new DashboardService();
    private final BackupService backupService = new BackupService();

    private List<PermissionRequest> allRequests = List.of();

    @FXML
    public void initialize() {
        User currentUser = SessionContext.getCurrentUser();
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getName());
            roleLabel.setText(currentUser.getRole().getDisplayName());
        }

        if (requestsTable != null && currentUser != null && currentUser.getRole() == UserRole.CLUB_REP) {
            configureRepresentativeTable();
            configureFilterCombo();
            loadRepresentativeRequests();
        }
    }

    private void configureRepresentativeTable() {
        idColumn.setCellValueFactory(cell ->
                new SimpleStringProperty("#" + cell.getValue().getRequestId()));
        titleColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getTitle()));
        categoryColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getPermissionType() != null
                        ? cell.getValue().getPermissionType().getTypeName()
                        : ""));
        eventDateColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getEventDate() != null
                        ? cell.getValue().getEventDate().format(DATE_FORMAT)
                        : "—"));
        statusColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getStatus().getDisplayName()));
        createdColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getCreatedAt() != null
                        ? cell.getValue().getCreatedAt().format(DATE_TIME_FORMAT)
                        : "—"));

        requestsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (submitDraftButton != null) {
                boolean canSubmit = newSel != null && newSel.getStatus() == RequestStatus.DRAFT;
                submitDraftButton.setDisable(!canSubmit);
            }
            if (viewDetailsButton != null) {
                viewDetailsButton.setDisable(newSel == null);
            }
        });

        requestsTable.setRowFactory(tv -> {
            TableRow<PermissionRequest> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    SceneNavigator.openRequestDetails(row.getItem().getRequestId());
                }
            });
            return row;
        });
    }

    private void configureFilterCombo() {
        statusFilterCombo.setItems(FXCollections.observableArrayList(
                "All Statuses",
                RequestStatus.DRAFT.getDisplayName(),
                RequestStatus.PENDING_FACULTY.getDisplayName(),
                RequestStatus.PENDING_DOSA.getDisplayName(),
                RequestStatus.PENDING_ASSOCIATE_DEAN.getDisplayName(),
                RequestStatus.APPROVED.getDisplayName(),
                RequestStatus.REJECTED.getDisplayName()
        ));
        statusFilterCombo.getSelectionModel().selectFirst();
        statusFilterCombo.setOnAction(event -> applyFilter());
    }

    private void loadRepresentativeRequests() {
        try {
            User currentUser = SessionContext.requireCurrentUser();
            allRequests = requestService.getRequestsForRepresentative(currentUser);
            updateKpiCards(allRequests);
            applyFilter();
        } catch (DatabaseException | UnauthorizedActionException ex) {
            LOGGER.log(Level.WARNING, "Unable to load representative requests.", ex);
            showAlert(Alert.AlertType.ERROR, "Load Error", ex.getMessage());
        }
    }

    private void updateKpiCards(List<PermissionRequest> requests) {
        Map<RequestStatus, Long> counts = dashboardService.getStatusCounts(requests);
        totalCountLabel.setText(String.valueOf(requests.size()));
        pendingCountLabel.setText(String.valueOf(dashboardService.countPending(counts)));
        approvedCountLabel.setText(String.valueOf(counts.getOrDefault(RequestStatus.APPROVED, 0L)));
        rejectedCountLabel.setText(String.valueOf(counts.getOrDefault(RequestStatus.REJECTED, 0L)));
    }

    private void applyFilter() {
        String selected = statusFilterCombo.getValue();
        if (selected == null || "All Statuses".equals(selected)) {
            requestsTable.setItems(FXCollections.observableArrayList(allRequests));
            return;
        }
        List<PermissionRequest> filtered = allRequests.stream()
                .filter(req -> req.getStatus() != null && selected.equals(req.getStatus().getDisplayName()))
                .toList();
        requestsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void handleNewRequest() {
        SceneNavigator.navigateTo("/fxml/request-form.fxml",
                "CPMS — New Permission Request");
    }

    @FXML
    private void handleSubmitDraft() {
        PermissionRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            requestService.submitDraftRequest(SessionContext.requireCurrentUser(), selected.getRequestId());
            showAlert(Alert.AlertType.INFORMATION, "Request Submitted",
                    "Request #" + selected.getRequestId() + " has been submitted to your Faculty Advisor.");
            loadRepresentativeRequests();
        } catch (ValidationException | UnauthorizedActionException | DatabaseException ex) {
            showAlert(Alert.AlertType.ERROR, "Submission Failed", ex.getMessage());
        }
    }

    @FXML
    private void handleViewDetails() {
        PermissionRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            SceneNavigator.openRequestDetails(selected.getRequestId());
        }
    }

    @FXML
    private void handleExportBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Requests Backup");
        chooser.setInitialFileName("cpms-requests-backup.ser");
        File file = chooser.showSaveDialog(SceneNavigator.getPrimaryStage());
        if (file != null) {
            try {
                backupService.exportRequestsBackup(allRequests, file.toPath());
                showAlert(Alert.AlertType.INFORMATION, "Backup Exported",
                        "Saved " + allRequests.size() + " request(s) to " + file.getName());
            } catch (ValidationException ex) {
                showAlert(Alert.AlertType.ERROR, "Export Failed", ex.getMessage());
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadRepresentativeRequests();
    }

    @FXML
    private void handleLogout() {
        authService.logout();
        SceneNavigator.navigateTo("/fxml/login.fxml",
                "Campus Permission Management System (CPMS) — Login");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
