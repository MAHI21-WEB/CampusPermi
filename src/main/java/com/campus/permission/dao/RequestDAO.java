package com.campus.permission.dao;

import com.campus.permission.enums.RequestStatus;
import com.campus.permission.model.PermissionRequest;

import java.sql.Connection;
import java.util.List;

/**
 * Data Access Object for permission requests.
 */
public interface RequestDAO extends BaseDAO<PermissionRequest> {

    List<PermissionRequest> findBySociety(int societyId);

    List<PermissionRequest> findBySubmittedBy(int userId);

    List<PermissionRequest> findPendingByStatus(RequestStatus status);

    List<PermissionRequest> findByFacultyAdvisorAndStatus(int facultyAdvisorId, RequestStatus status);

    void update(PermissionRequest request);

    void updateStatus(Connection connection, int requestId, RequestStatus newStatus);
}
