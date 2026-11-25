package com.campus.permission.service;

import com.campus.permission.dao.ApprovalDAO;
import com.campus.permission.dao.RequestDAO;
import com.campus.permission.dao.SocietyDAO;
import com.campus.permission.enums.ApprovalLevel;
import com.campus.permission.enums.Decision;
import com.campus.permission.enums.RequestStatus;
import com.campus.permission.enums.UserRole;
import com.campus.permission.exception.DatabaseException;
import com.campus.permission.exception.UnauthorizedActionException;
import com.campus.permission.exception.ValidationException;
import com.campus.permission.model.Approval;
import com.campus.permission.model.PermissionRequest;
import com.campus.permission.model.Society;
import com.campus.permission.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ApprovalServiceTest {

    private ApprovalService approvalService;
    private PermissionRequest request;
    private List<Approval> approvals;
    private User facultyAssigned;
    private User facultyOther;
    private User dosaRep;
    private User assocDean;
    private AtomicBoolean committed;
    private AtomicBoolean rolledBack;
    private AtomicBoolean failNextUpdate;

    @BeforeEach
    void setUp() {
        approvals = new ArrayList<>();
        committed = new AtomicBoolean(false);
        rolledBack = new AtomicBoolean(false);
        failNextUpdate = new AtomicBoolean(false);

        facultyAssigned = new User(1, "Dr. Rajesh Sharma", "rajesh@thapar.edu", "h", UserRole.FACULTY_ADVISOR, null);
        facultyOther = new User(2, "Dr. Meera Verma", "meera@thapar.edu", "h", UserRole.FACULTY_ADVISOR, null);
        dosaRep = new User(3, "Anil Kapoor", "dosa@thapar.edu", "h", UserRole.DOSA_REP, null);
        assocDean = new User(4, "Prof. Sunita Malhotra", "dean@thapar.edu", "h", UserRole.ASSOCIATE_DEAN, null);

        request = new PermissionRequest();
        request.setRequestId(10);
        request.setSocietyId(1);
        request.setSubmittedBy(5);
        request.setTitle("HackTU 2026");
        request.setDescription("Annual hackathon");
        request.setStatus(RequestStatus.PENDING_FACULTY);

        RequestDAO stubRequestDAO = new RequestDAO() {
            @Override
            public Optional<PermissionRequest> findById(int id) {
                return id == 10 ? Optional.of(request) : Optional.empty();
            }

            @Override
            public void updateStatus(Connection connection, int requestId, RequestStatus newStatus) {
                if (failNextUpdate.get()) {
                    throw new DatabaseException("Simulated DB failure midway");
                }
                request.setStatus(newStatus);
            }

            @Override public PermissionRequest save(PermissionRequest entity) { return entity; }
            @Override public List<PermissionRequest> findAll() { return List.of(request); }
            @Override public List<PermissionRequest> findBySociety(int societyId) { return List.of(); }
            @Override public List<PermissionRequest> findBySubmittedBy(int userId) { return List.of(); }
            @Override public List<PermissionRequest> findPendingByStatus(RequestStatus status) { return List.of(); }
            @Override public List<PermissionRequest> findByFacultyAdvisorAndStatus(int f, RequestStatus s) { return List.of(); }
            @Override public void update(PermissionRequest r) { }
        };

        ApprovalDAO stubApprovalDAO = new ApprovalDAO() {
            private int seq = 1;

            @Override
            public Approval save(Connection connection, Approval approval) {
                approval.setApprovalId(seq++);
                approvals.add(approval);
                return approval;
            }

            @Override public Approval save(Approval entity) { return save(null, entity); }
            @Override public Optional<Approval> findById(int id) { return Optional.empty(); }
            @Override public List<Approval> findAll() { return approvals; }
            @Override public List<Approval> findByRequestId(int requestId) { return approvals; }
            @Override public List<Approval> findByApproverId(int approverId) { return List.of(); }
            @Override public List<Approval> findByApprovalLevel(ApprovalLevel level) { return List.of(); }
        };

        SocietyDAO stubSocietyDAO = new SocietyDAO() {
            @Override
            public Optional<Society> findById(int id) {
                return Optional.of(new Society(1, "CCS", "Tech society", 1));
            }

            @Override public Society save(Society entity) { return entity; }
            @Override public List<Society> findAll() { return List.of(); }
            @Override public List<Society> findByFacultyAdvisor(int facultyAdvisorId) { return List.of(); }
        };

        Connection proxyConn = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAutoCommit" -> true;
                    case "commit" -> { committed.set(true); yield null; }
                    case "rollback" -> { rolledBack.set(true); yield null; }
                    default -> null;
                }
        );

        approvalService = new ApprovalService(stubRequestDAO, stubApprovalDAO, stubSocietyDAO, () -> proxyConn);
    }

    @Test
    void fullSequentialApprovalLifecycleReachesApproved() {
        approvalService.processDecision(facultyAssigned, 10, Decision.APPROVED, "Recommended");
        assertEquals(RequestStatus.PENDING_DOSA, request.getStatus());

        approvalService.processDecision(dosaRep, 10, Decision.APPROVED, "Verified");
        assertEquals(RequestStatus.PENDING_ASSOCIATE_DEAN, request.getStatus());

        approvalService.processDecision(assocDean, 10, Decision.APPROVED, "Final approval");
        assertEquals(RequestStatus.APPROVED, request.getStatus());

        assertEquals(3, approvals.size());
        assertTrue(committed.get());
        assertFalse(rolledBack.get());
    }

    @Test
    void facultyRejectionMovesToRejectedAndBlocksFurtherApproval() {
        approvalService.processDecision(facultyAssigned, 10, Decision.REJECTED, "Incomplete details");
        assertEquals(RequestStatus.REJECTED, request.getStatus());

        assertThrows(ValidationException.class,
                () -> approvalService.processDecision(dosaRep, 10, Decision.APPROVED, "Try bypass"));
    }

    @Test
    void dosaCannotApproveFacultyPendingRequest() {
        assertThrows(UnauthorizedActionException.class,
                () -> approvalService.processDecision(dosaRep, 10, Decision.APPROVED, "Bypass"));
        assertEquals(RequestStatus.PENDING_FACULTY, request.getStatus());
    }

    @Test
    void unassignedFacultyAdvisorCannotApprove() {
        assertThrows(UnauthorizedActionException.class,
                () -> approvalService.processDecision(facultyOther, 10, Decision.APPROVED, "Not my society"));
        assertEquals(RequestStatus.PENDING_FACULTY, request.getStatus());
    }

    @Test
    void rejectionWithoutRemarksThrowsValidationException() {
        assertThrows(ValidationException.class,
                () -> approvalService.processDecision(facultyAssigned, 10, Decision.REJECTED, "  "));
    }

    @Test
    void midTransactionFailureRollsBackAndThrowsDatabaseException() {
        failNextUpdate.set(true);

        assertThrows(DatabaseException.class,
                () -> approvalService.processDecision(facultyAssigned, 10, Decision.APPROVED, "Ok"));

        assertTrue(rolledBack.get());
        assertFalse(committed.get());
        assertEquals(RequestStatus.PENDING_FACULTY, request.getStatus());
    }
}
