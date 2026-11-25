package com.campus.permission.service;

import com.campus.permission.dao.ApprovalDAO;
import com.campus.permission.dao.RequestDAO;
import com.campus.permission.dao.SocietyDAO;
import com.campus.permission.dao.impl.ApprovalDAOImpl;
import com.campus.permission.dao.impl.RequestDAOImpl;
import com.campus.permission.dao.impl.SocietyDAOImpl;
import com.campus.permission.enums.ApprovalLevel;
import com.campus.permission.enums.Decision;
import com.campus.permission.enums.RequestStatus;
import com.campus.permission.enums.UserRole;
import com.campus.permission.exception.DatabaseException;
import com.campus.permission.exception.ResourceNotFoundException;
import com.campus.permission.exception.UnauthorizedActionException;
import com.campus.permission.exception.ValidationException;
import com.campus.permission.model.Approval;
import com.campus.permission.model.PermissionRequest;
import com.campus.permission.model.Society;
import com.campus.permission.model.User;
import com.campus.permission.util.DBConnection;
import com.campus.permission.util.ValidationUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central sequential approval workflow engine for CPMS.
 * Enforces role-based authorization, stage transitions, and atomic JDBC transactions.
 */
public class ApprovalService {

    private static final Logger LOGGER = Logger.getLogger(ApprovalService.class.getName());

    private final RequestDAO requestDAO;
    private final ApprovalDAO approvalDAO;
    private final SocietyDAO societyDAO;
    private final Supplier<Connection> connectionSupplier;

    public ApprovalService() {
        this(new RequestDAOImpl(), new ApprovalDAOImpl(), new SocietyDAOImpl(), DBConnection::getConnection);
    }

    public ApprovalService(RequestDAO requestDAO,
                           ApprovalDAO approvalDAO,
                           SocietyDAO societyDAO,
                           Supplier<Connection> connectionSupplier) {
        this.requestDAO = requestDAO;
        this.approvalDAO = approvalDAO;
        this.societyDAO = societyDAO;
        this.connectionSupplier = connectionSupplier;
    }

    /**
     * Processes an approval or rejection decision inside a single database transaction.
     */
    public Approval processDecision(User approver, int requestId, Decision decision, String remarks) {
        if (approver == null) {
            throw new UnauthorizedActionException("Authentication is required.");
        }
        if (decision == null) {
            throw new ValidationException("Approval decision is required.");
        }

        String cleanRemarks = remarks == null ? "" : remarks.trim();
        if (decision == Decision.REJECTED && ValidationUtil.isBlank(cleanRemarks)) {
            throw new ValidationException("Remarks are required when rejecting a request.");
        }

        PermissionRequest request = requestDAO.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission request not found."));

        if (request.getStatus() == RequestStatus.DRAFT
                || request.getStatus() == RequestStatus.APPROVED
                || request.getStatus() == RequestStatus.REJECTED) {
            throw new ValidationException("Request has already been processed or is not yet submitted.");
        }

        WorkflowTransition transition = resolveTransition(approver, request);
        RequestStatus nextStatus = (decision == Decision.APPROVED)
                ? transition.nextApprovedStatus
                : RequestStatus.REJECTED;

        Approval approval = new Approval();
        approval.setRequestId(requestId);
        approval.setApproverId(approver.getUserId());
        approval.setApprovalLevel(transition.level);
        approval.setDecision(decision);
        approval.setRemarks(cleanRemarks);

        executeInTransaction(approval, requestId, nextStatus);

        LOGGER.info("Processed " + decision + " for requestId=" + requestId
                + " at level=" + transition.level
                + " by userId=" + approver.getUserId()
                + " -> newStatus=" + nextStatus);
        return approval;
    }

    /**
     * Returns requests currently awaiting review by the given authority user.
     */
    public List<PermissionRequest> getPendingRequestsForApprover(User approver) {
        if (approver == null) {
            throw new UnauthorizedActionException("Authentication is required.");
        }
        return switch (approver.getRole()) {
            case FACULTY_ADVISOR ->
                    requestDAO.findByFacultyAdvisorAndStatus(approver.getUserId(), RequestStatus.PENDING_FACULTY);
            case DOSA_REP ->
                    requestDAO.findPendingByStatus(RequestStatus.PENDING_DOSA);
            case ASSOCIATE_DEAN ->
                    requestDAO.findPendingByStatus(RequestStatus.PENDING_ASSOCIATE_DEAN);
            case CLUB_REP ->
                    throw new UnauthorizedActionException("Club representatives cannot access approval queues.");
        };
    }

    /**
     * Returns distinct requests previously reviewed by the given approver, sorted newest first.
     */
    public List<PermissionRequest> getReviewedRequestsForApprover(User approver) {
        if (approver == null || approver.getRole() == UserRole.CLUB_REP) {
            throw new UnauthorizedActionException("Access denied.");
        }
        List<Approval> approvals = approvalDAO.findByApproverId(approver.getUserId());
        Set<Integer> seenRequestIds = new HashSet<>();
        List<PermissionRequest> reviewed = new ArrayList<>();
        for (Approval approval : approvals) {
            if (seenRequestIds.add(approval.getRequestId())) {
                requestDAO.findById(approval.getRequestId()).ifPresent(reviewed::add);
            }
        }
        reviewed.sort(Comparator.comparing(
                PermissionRequest::getUpdatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return reviewed;
    }

    /**
     * Returns chronological approval history for a request.
     */
    public List<Approval> getApprovalHistory(int requestId) {
        return approvalDAO.findByRequestId(requestId);
    }

    /**
     * Checks whether the given user is currently eligible to approve/reject the request.
     */
    public boolean canUserActOnRequest(User user, PermissionRequest request) {
        if (user == null || request == null || request.getStatus() == null) {
            return false;
        }
        try {
            resolveTransition(user, request);
            return true;
        } catch (UnauthorizedActionException | ResourceNotFoundException ex) {
            return false;
        }
    }

    private WorkflowTransition resolveTransition(User approver, PermissionRequest request) {
        return switch (approver.getRole()) {
            case FACULTY_ADVISOR -> {
                if (request.getStatus() != RequestStatus.PENDING_FACULTY) {
                    throw new UnauthorizedActionException("Request is not awaiting Faculty Advisor review.");
                }
                Society society = societyDAO.findById(request.getSocietyId())
                        .orElseThrow(() -> new ResourceNotFoundException("Associated society not found."));
                if (society.getFacultyAdvisorId() != approver.getUserId()) {
                    throw new UnauthorizedActionException("You are not the assigned Faculty Advisor for this society.");
                }
                yield new WorkflowTransition(ApprovalLevel.FACULTY, RequestStatus.PENDING_DOSA);
            }
            case DOSA_REP -> {
                if (request.getStatus() != RequestStatus.PENDING_DOSA) {
                    throw new UnauthorizedActionException("Only faculty-approved requests can enter DOSA review.");
                }
                yield new WorkflowTransition(ApprovalLevel.DOSA, RequestStatus.PENDING_ASSOCIATE_DEAN);
            }
            case ASSOCIATE_DEAN -> {
                if (request.getStatus() != RequestStatus.PENDING_ASSOCIATE_DEAN) {
                    throw new UnauthorizedActionException("Only DOSA-approved requests can reach the Associate Dean.");
                }
                yield new WorkflowTransition(ApprovalLevel.ASSOCIATE_DEAN, RequestStatus.APPROVED);
            }
            case CLUB_REP ->
                    throw new UnauthorizedActionException("Club representatives are not authorized to approve requests.");
        };
    }

    private void executeInTransaction(Approval approval, int requestId, RequestStatus nextStatus) {
        Connection conn = null;
        boolean previousAutoCommit = true;
        try {
            conn = connectionSupplier.get();
            previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            approvalDAO.save(conn, approval);
            requestDAO.updateStatus(conn, requestId, nextStatus);

            conn.commit();
        } catch (RuntimeException | SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                    LOGGER.warning("Rolled back approval transaction for requestId=" + requestId);
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Failed to rollback approval transaction.", rollbackEx);
                }
            }
            if (ex instanceof DatabaseException dbEx) {
                throw dbEx;
            }
            LOGGER.log(Level.SEVERE, "Approval transaction failed for requestId=" + requestId, ex);
            throw new DatabaseException("Unable to complete approval decision. No changes were saved.", ex);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(previousAutoCommit);
                    conn.close();
                } catch (SQLException closeEx) {
                    LOGGER.log(Level.WARNING, "Failed to close database connection cleanly.", closeEx);
                }
            }
        }
    }

    private record WorkflowTransition(ApprovalLevel level, RequestStatus nextApprovedStatus) {
    }
}
