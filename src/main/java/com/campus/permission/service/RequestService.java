package com.campus.permission.service;

import com.campus.permission.dao.PermissionTypeDAO;
import com.campus.permission.dao.RequestDAO;
import com.campus.permission.dao.SocietyDAO;
import com.campus.permission.dao.impl.PermissionTypeDAOImpl;
import com.campus.permission.dao.impl.RequestDAOImpl;
import com.campus.permission.dao.impl.SocietyDAOImpl;
import com.campus.permission.enums.RequestStatus;
import com.campus.permission.enums.UserRole;
import com.campus.permission.exception.ResourceNotFoundException;
import com.campus.permission.exception.UnauthorizedActionException;
import com.campus.permission.exception.ValidationException;
import com.campus.permission.model.PermissionRequest;
import com.campus.permission.model.PermissionType;
import com.campus.permission.model.Society;
import com.campus.permission.model.User;
import com.campus.permission.util.ValidationUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Business logic for creating, validating, submitting, and retrieving permission requests.
 */
public class RequestService {

    private static final Logger LOGGER = Logger.getLogger(RequestService.class.getName());
    private static final int MAX_TITLE_LENGTH = 200;

    private final RequestDAO requestDAO;
    private final PermissionTypeDAO permissionTypeDAO;
    private final SocietyDAO societyDAO;

    public RequestService() {
        this(new RequestDAOImpl(), new PermissionTypeDAOImpl(), new SocietyDAOImpl());
    }

    public RequestService(RequestDAO requestDAO, PermissionTypeDAO permissionTypeDAO, SocietyDAO societyDAO) {
        this.requestDAO = requestDAO;
        this.permissionTypeDAO = permissionTypeDAO;
        this.societyDAO = societyDAO;
    }

    /**
     * Returns all active database-configured permission categories.
     */
    public List<PermissionType> getActivePermissionTypes() {
        return permissionTypeDAO.findAllActive();
    }

    /**
     * Creates a new permission request as either DRAFT or PENDING_FACULTY.
     */
    public PermissionRequest createRequest(User currentUser,
                                           PermissionType permissionType,
                                           String title,
                                           String description,
                                           LocalDate eventDate,
                                           LocalTime startTime,
                                           LocalTime endTime,
                                           boolean submitImmediately) {
        requireRepresentative(currentUser);

        if (permissionType == null || !permissionType.isActive()) {
            throw new ValidationException("Please select an active permission category.");
        }
        String cleanTitle = ValidationUtil.requireNonBlank(title, "Request title");
        if (cleanTitle.length() > MAX_TITLE_LENGTH) {
            throw new ValidationException("Request title must not exceed " + MAX_TITLE_LENGTH + " characters.");
        }
        String cleanDescription = ValidationUtil.requireNonBlank(description, "Description");

        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new ValidationException("End time must be after start time.");
        }

        PermissionRequest request = new PermissionRequest();
        request.setSocietyId(currentUser.getSocietyId());
        request.setSubmittedBy(currentUser.getUserId());
        request.setPermissionType(permissionType);
        request.setTitle(cleanTitle);
        request.setDescription(cleanDescription);
        request.setEventDate(eventDate);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setStatus(submitImmediately ? RequestStatus.PENDING_FACULTY : RequestStatus.DRAFT);

        PermissionRequest saved = requestDAO.save(request);
        LOGGER.info("Created requestId=" + saved.getRequestId()
                + " with status=" + saved.getStatus()
                + " by userId=" + currentUser.getUserId());
        return saved;
    }

    /**
     * Submits an existing DRAFT request into the approval workflow (PENDING_FACULTY).
     */
    public void submitDraftRequest(User currentUser, int requestId) {
        requireRepresentative(currentUser);

        PermissionRequest request = requestDAO.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission request not found."));

        if (!Objects.equals(currentUser.getSocietyId(), request.getSocietyId())) {
            throw new UnauthorizedActionException("You are not authorized to submit requests for another society.");
        }
        if (request.getStatus() != RequestStatus.DRAFT) {
            throw new ValidationException("Only draft requests can be submitted.");
        }

        request.setStatus(RequestStatus.PENDING_FACULTY);
        requestDAO.update(request);
        LOGGER.info("Submitted draft requestId=" + requestId + " to PENDING_FACULTY.");
    }

    /**
     * Retrieves all requests belonging to the representative's society, sorted newest first.
     */
    public List<PermissionRequest> getRequestsForRepresentative(User currentUser) {
        requireRepresentative(currentUser);
        List<PermissionRequest> requests = requestDAO.findBySociety(currentUser.getSocietyId());
        requests.sort(Comparator.comparing(
                PermissionRequest::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return requests;
    }

    /**
     * Retrieves a single request after enforcing role-based read access.
     */
    public PermissionRequest getRequestForUser(User currentUser, int requestId) {
        if (currentUser == null) {
            throw new UnauthorizedActionException("Authentication is required.");
        }
        PermissionRequest request = requestDAO.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission request not found."));

        switch (currentUser.getRole()) {
            case CLUB_REP -> {
                if (!Objects.equals(currentUser.getSocietyId(), request.getSocietyId())) {
                    throw new UnauthorizedActionException("Access denied: request belongs to another society.");
                }
            }
            case FACULTY_ADVISOR -> {
                Society society = societyDAO.findById(request.getSocietyId())
                        .orElseThrow(() -> new ResourceNotFoundException("Associated society not found."));
                if (society.getFacultyAdvisorId() != currentUser.getUserId()) {
                    throw new UnauthorizedActionException("Access denied: society is not assigned to you.");
                }
            }
            case DOSA_REP, ASSOCIATE_DEAN -> {
                // Administrative reviewers can inspect requests in the workflow.
            }
        }
        return request;
    }

    private void requireRepresentative(User user) {
        if (user == null || user.getRole() != UserRole.CLUB_REP) {
            throw new UnauthorizedActionException("Only a Club/Society Representative can perform this action.");
        }
        if (user.getSocietyId() == null) {
            throw new UnauthorizedActionException("Representative account is not linked to any society.");
        }
    }
}
