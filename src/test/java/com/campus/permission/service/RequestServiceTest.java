package com.campus.permission.service;

import com.campus.permission.dao.PermissionTypeDAO;
import com.campus.permission.dao.RequestDAO;
import com.campus.permission.dao.SocietyDAO;
import com.campus.permission.enums.RequestStatus;
import com.campus.permission.enums.UserRole;
import com.campus.permission.exception.UnauthorizedActionException;
import com.campus.permission.exception.ValidationException;
import com.campus.permission.model.PermissionRequest;
import com.campus.permission.model.PermissionType;
import com.campus.permission.model.Society;
import com.campus.permission.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RequestServiceTest {

    private RequestService requestService;
    private DashboardService dashboardService;
    private User repSociety1;
    private User repSociety2;
    private PermissionType activeType;
    private List<PermissionRequest> store;

    @BeforeEach
    void setUp() {
        store = new ArrayList<>();
        repSociety1 = new User(5, "Aarav Singh", "aarav.ccs@thapar.edu", "hash", UserRole.CLUB_REP, 1);
        repSociety2 = new User(6, "Priya Nair", "priya.acm@thapar.edu", "hash", UserRole.CLUB_REP, 2);
        activeType = new PermissionType(1, "Event/Activity Permission", "General events", true);

        RequestDAO stubRequestDAO = new RequestDAO() {
            private int seq = 1;

            @Override
            public PermissionRequest save(PermissionRequest entity) {
                entity.setRequestId(seq++);
                entity.setCreatedAt(LocalDateTime.now());
                store.add(entity);
                return entity;
            }

            @Override
            public Optional<PermissionRequest> findById(int id) {
                return store.stream().filter(r -> r.getRequestId() == id).findFirst();
            }

            @Override
            public List<PermissionRequest> findAll() {
                return new ArrayList<>(store);
            }

            @Override
            public List<PermissionRequest> findBySociety(int societyId) {
                return new ArrayList<>(store.stream().filter(r -> r.getSocietyId() == societyId).toList());
            }

            @Override
            public List<PermissionRequest> findBySubmittedBy(int userId) {
                return List.of();
            }

            @Override
            public List<PermissionRequest> findPendingByStatus(RequestStatus status) {
                return List.of();
            }

            @Override
            public List<PermissionRequest> findByFacultyAdvisorAndStatus(int facultyAdvisorId, RequestStatus status) {
                return List.of();
            }

            @Override
            public void update(PermissionRequest request) {
                // In-memory reference already updated
            }

            @Override
            public void updateStatus(Connection connection, int requestId, RequestStatus newStatus) {
                findById(requestId).ifPresent(r -> r.setStatus(newStatus));
            }
        };

        PermissionTypeDAO stubTypeDAO = new PermissionTypeDAO() {
            @Override
            public List<PermissionType> findAllActive() {
                return List.of(activeType);
            }

            @Override
            public PermissionType save(PermissionType entity) {
                return entity;
            }

            @Override
            public Optional<PermissionType> findById(int id) {
                return Optional.of(activeType);
            }

            @Override
            public List<PermissionType> findAll() {
                return List.of(activeType);
            }
        };

        SocietyDAO stubSocietyDAO = new SocietyDAO() {
            @Override
            public List<Society> findByFacultyAdvisor(int facultyAdvisorId) {
                return List.of();
            }

            @Override
            public Society save(Society entity) {
                return entity;
            }

            @Override
            public Optional<Society> findById(int id) {
                return Optional.of(new Society(id, "Society " + id, "Desc", 1));
            }

            @Override
            public List<Society> findAll() {
                return List.of();
            }
        };

        requestService = new RequestService(stubRequestDAO, stubTypeDAO, stubSocietyDAO);
        dashboardService = new DashboardService();
    }

    @Test
    void representativeCreatesAndSubmitsRequest() {
        PermissionRequest created = requestService.createRequest(
                repSociety1, activeType, "HackTU 2026", "Annual 24h hackathon",
                LocalDate.of(2026, 12, 10), LocalTime.of(9, 0), LocalTime.of(18, 0), true
        );

        assertEquals(1, created.getRequestId());
        assertEquals(RequestStatus.PENDING_FACULTY, created.getStatus());
        assertEquals(1, created.getSocietyId());
    }

    @Test
    void representativeCanSaveDraftAndSubmitLater() {
        PermissionRequest draft = requestService.createRequest(
                repSociety1, activeType, "Tech Talk", "Guest talk on Java",
                LocalDate.of(2026, 12, 12), null, null, false
        );
        assertEquals(RequestStatus.DRAFT, draft.getStatus());

        requestService.submitDraftRequest(repSociety1, draft.getRequestId());
        assertEquals(RequestStatus.PENDING_FACULTY, draft.getStatus());
    }

    @Test
    void incompleteOrInvalidTimeRangeThrowsValidationException() {
        assertThrows(ValidationException.class, () -> requestService.createRequest(
                repSociety1, activeType, "   ", "Description",
                null, null, null, true
        ));

        assertThrows(ValidationException.class, () -> requestService.createRequest(
                repSociety1, activeType, "Valid Title", "Description",
                LocalDate.of(2026, 12, 10), LocalTime.of(16, 0), LocalTime.of(14, 0), true
        ));
    }

    @Test
    void representativeAccessingAnotherSocietysRequestIsDenied() {
        PermissionRequest created = requestService.createRequest(
                repSociety1, activeType, "CCS Event", "Details",
                null, null, null, true
        );

        assertThrows(UnauthorizedActionException.class,
                () -> requestService.getRequestForUser(repSociety2, created.getRequestId()));
    }

    @Test
    void dashboardServiceAggregatesStatusCountsUsingHashMap() {
        requestService.createRequest(repSociety1, activeType, "Req 1", "Desc 1", null, null, null, true);
        requestService.createRequest(repSociety1, activeType, "Req 2", "Desc 2", null, null, null, false);

        List<PermissionRequest> requests = requestService.getRequestsForRepresentative(repSociety1);
        Map<RequestStatus, Long> counts = dashboardService.getStatusCounts(requests);

        assertEquals(1L, counts.get(RequestStatus.PENDING_FACULTY));
        assertEquals(1L, counts.get(RequestStatus.DRAFT));
        assertEquals(1L, dashboardService.countPending(counts));
    }
}
