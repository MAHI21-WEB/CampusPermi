package com.campus.permission.controller;

import com.campus.permission.enums.RequestStatus;
import com.campus.permission.exception.DatabaseException;
import com.campus.permission.exception.UnauthorizedActionException;
import com.campus.permission.model.PermissionRequest;
import com.campus.permission.model.User;
import com.campus.permission.service.ApprovalService;
import com.campus.permission.service.AuthService;
import com.campus.permission.service.DashboardService;
import com.campus.permission.util.SceneNavigator;
import com.campus.permission.util.SessionContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for Faculty Advisor, DOSA Representative, and Associate Dean dashboards.
 */
public class ApprovalController {

    private static final Logger LOGGER = Logger.getLogger(ApprovalController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @FXML
    private Label userNameLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label pendingCountLabel;

    @FXML
    private Label reviewedCountLabel;

    @FXML
    private Label approvedCountLabel;

    @FXML
    private Label rejectedCountLabel;

    @FXML
    private TableView<PermissionRequest> pendingTable;

    @FXML
    private TableColumn<PermissionRequest, String> pendingIdColumn;

    @FXML
    private TableColumn<PermissionRequest, String> pendingSocietyColumn;

    @FXML
    private TableColumn<PermissionRequest, String> pendingTitleColumn;

    @FXML
    private TableColumn<PermissionRequest, String> pendingCategoryColumn;

    @FXML
    private TableColumn<PermissionRequest, String> pendingEventDateColumn;

    @FXML
    private TableColumn<PermissionRequest, String> pendingSubmittedColumn;

    @FXML
    private TableView<PermissionRequest> reviewedTable;

    @FXML
    private TableColumn<PermissionRequest, String> revIdColumn;

    @FXML
    private TableColumn<PermissionRequest, String> revSocietyColumn;

    @FXML
    private TableColumn<PermissionRequest, String> revTitleColumn;

    @FXML
    private TableColumn<PermissionRequest, String> revCategoryColumn;

    @FXML
    private TableColumn<PermissionRequest, String> revStatusColumn;

    @FXML
    private TableColumn<PermissionRequest, String> revUpdatedColumn;

    @FXML
    private Button reviewSelectedButton;

    private final AuthService authService = new AuthService();
    private final ApprovalService approvalService = new ApprovalService();
    private final DashboardService dashboardService = new DashboardService();

    @FXML
    public void initialize() {
        User currentUser = SessionContext.getCurrentUser();
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getName());
            roleLabel.setText(currentUser.getRole().getDisplayName());
        }

        configureTable(pendingTable, pendingIdColumn, pendingSocietyColumn, pendingTitleColumn,
                pendingCategoryColumn, pendingEventDateColumn, pendingSubmittedColumn, true);
        configureTable(reviewedTable, revIdColumn, revSocietyColumn, revTitleColumn,
                revCategoryColumn, revStatusColumn, revUpdatedColumn, false);

        if (reviewSelectedButton != null) {
            pendingTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) ->
                    reviewSelectedButton.setDisable(newSel == null));
        }

        loadQueues();
    }

    private void configureTable(TableView<PermissionRequest> table,
                                TableColumn<PermissionRequest, String> idCol,
                                TableColumn<PermissionRequest, String> societyCol,
                                TableColumn<PermissionRequest, String> titleCol,
                                TableColumn<PermissionRequest, String> categoryCol,
                                TableColumn<PermissionRequest, String> fifthCol,
                                TableColumn<PermissionRequest, String> sixthCol,
                                boolean isPendingTable) {
        idCol.setCellValueFactory(cell ->
                new SimpleStringProperty("#" + cell.getValue().getRequestId()));
        societyCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getSocietyName()));
        titleCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getTitle()));
        categoryCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getPermissionType() != null
                        ? cell.getValue().getPermissionType().getTypeName()
                        : ""));

        if (isPendingTable) {
            fifthCol.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getEventDate() != null
                            ? cell.getValue().getEventDate().format(DATE_FORMAT)
                            : "—"));
            sixthCol.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getCreatedAt() != null
                            ? cell.getValue().getCreatedAt().format(DATE_TIME_FORMAT)
                            : "—"));
        } else {
            fifthCol.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getStatus().getDisplayName()));
            sixthCol.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getUpdatedAt() != null
                            ? cell.getValue().getUpdatedAt().format(DATE_TIME_FORMAT)
                            : "—"));
        }

        table.setRowFactory(tv -> {
            TableRow<PermissionRequest> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    SceneNavigator.openRequestDetails(row.getItem().getRequestId());
                }
            });
            return row;
        });
    }

    private void loadQueues() {
        try {
            User currentUser = SessionContext.requireCurrentUser();
            List<PermissionRequest> pending = approvalService.getPendingRequestsForApprover(currentUser);
            List<PermissionRequest> reviewed = approvalService.getReviewedRequestsForApprover(currentUser);

            pendingTable.setItems(FXCollections.observableArrayList(pending));
            reviewedTable.setItems(FXCollections.observableArrayList(reviewed));

            Map<RequestStatus, Long> reviewedCounts = dashboardService.getStatusCounts(reviewed);
            pendingCountLabel.setText(String.valueOf(pending.size()));
            reviewedCountLabel.setText(String.valueOf(reviewed.size()));
            approvedCountLabel.setText(String.valueOf(reviewedCounts.getOrDefault(RequestStatus.APPROVED, 0L)));
            rejectedCountLabel.setText(String.valueOf(reviewedCounts.getOrDefault(RequestStatus.REJECTED, 0L)));
        } catch (DatabaseException | UnauthorizedActionException ex) {
            LOGGER.log(Level.WARNING, "Failed to load approval queues.", ex);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Load Error");
            alert.setHeaderText(null);
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleReviewSelected() {
        PermissionRequest selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            selected = reviewedTable.getSelectionModel().getSelectedItem();
        }
        if (selected != null) {
            SceneNavigator.openRequestDetails(selected.getRequestId());
        }
    }

    @FXML
    private void handleRefresh() {
        loadQueues();
    }

    @FXML
    private void handleLogout() {
        authService.logout();
        SceneNavigator.navigateTo("/fxml/login.fxml",
                "Campus Permission Management System (CPMS) — Login");
    }
}
