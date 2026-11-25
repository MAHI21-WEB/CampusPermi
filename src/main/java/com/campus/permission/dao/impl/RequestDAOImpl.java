package com.campus.permission.dao.impl;

import com.campus.permission.dao.RequestDAO;
import com.campus.permission.enums.RequestStatus;
import com.campus.permission.exception.DatabaseException;
import com.campus.permission.model.PermissionRequest;
import com.campus.permission.model.PermissionType;
import com.campus.permission.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of {@link RequestDAO}.
 */
public class RequestDAOImpl implements RequestDAO {

    private static final Logger LOGGER = Logger.getLogger(RequestDAOImpl.class.getName());

    private static final String BASE_SELECT =
            "SELECT r.request_id, r.society_id, r.submitted_by, r.type_id, r.title, r.description, " +
            "       r.event_date, r.start_time, r.end_time, r.status, r.created_at, r.updated_at, " +
            "       s.society_name, u.name AS submitted_by_name, " +
            "       pt.type_name, pt.description AS type_description, pt.is_active " +
            "FROM Requests r " +
            "JOIN Societies s ON r.society_id = s.society_id " +
            "JOIN Users u ON r.submitted_by = u.user_id " +
            "JOIN Permission_Types pt ON r.type_id = pt.type_id ";

    @Override
    public PermissionRequest save(PermissionRequest request) {
        String sql = "INSERT INTO Requests " +
                "(society_id, submitted_by, type_id, title, description, event_date, start_time, end_time, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindRequestFields(ps, request);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    request.setRequestId(keys.getInt(1));
                }
            }
            return request;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to save permission request.", ex);
            throw new DatabaseException("Unable to save permission request.", ex);
        }
    }

    @Override
    public Optional<PermissionRequest> findById(int id) {
        String sql = BASE_SELECT + "WHERE r.request_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to find request by id: " + id, ex);
            throw new DatabaseException("Unable to retrieve permission request.", ex);
        }
    }

    @Override
    public List<PermissionRequest> findAll() {
        return queryList(BASE_SELECT + "ORDER BY r.created_at DESC", ps -> { });
    }

    @Override
    public List<PermissionRequest> findBySociety(int societyId) {
        String sql = BASE_SELECT + "WHERE r.society_id = ? ORDER BY r.created_at DESC";
        return queryList(sql, ps -> ps.setInt(1, societyId));
    }

    @Override
    public List<PermissionRequest> findBySubmittedBy(int userId) {
        String sql = BASE_SELECT + "WHERE r.submitted_by = ? ORDER BY r.created_at DESC";
        return queryList(sql, ps -> ps.setInt(1, userId));
    }

    @Override
    public List<PermissionRequest> findPendingByStatus(RequestStatus status) {
        String sql = BASE_SELECT + "WHERE r.status = ? ORDER BY r.created_at ASC";
        return queryList(sql, ps -> ps.setString(1, status.name()));
    }

    @Override
    public List<PermissionRequest> findByFacultyAdvisorAndStatus(int facultyAdvisorId, RequestStatus status) {
        String sql = BASE_SELECT +
                "WHERE s.faculty_advisor_id = ? AND r.status = ? ORDER BY r.created_at ASC";
        return queryList(sql, ps -> {
            ps.setInt(1, facultyAdvisorId);
            ps.setString(2, status.name());
        });
    }

    @Override
    public void update(PermissionRequest request) {
        String sql = "UPDATE Requests SET society_id = ?, submitted_by = ?, type_id = ?, title = ?, " +
                "description = ?, event_date = ?, start_time = ?, end_time = ?, status = ? " +
                "WHERE request_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindRequestFields(ps, request);
            ps.setInt(10, request.getRequestId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to update request: " + request.getRequestId(), ex);
            throw new DatabaseException("Unable to update permission request.", ex);
        }
    }

    @Override
    public void updateStatus(Connection connection, int requestId, RequestStatus newStatus) {
        String sql = "UPDATE Requests SET status = ? WHERE request_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, requestId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new DatabaseException("Request not found while updating status: " + requestId);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to update status for request: " + requestId, ex);
            throw new DatabaseException("Unable to update request status.", ex);
        }
    }

    private void bindRequestFields(PreparedStatement ps, PermissionRequest request) throws SQLException {
        ps.setInt(1, request.getSocietyId());
        ps.setInt(2, request.getSubmittedBy());
        ps.setInt(3, request.getPermissionType().getTypeId());
        ps.setString(4, request.getTitle());
        ps.setString(5, request.getDescription());
        if (request.getEventDate() != null) {
            ps.setDate(6, Date.valueOf(request.getEventDate()));
        } else {
            ps.setNull(6, Types.DATE);
        }
        if (request.getStartTime() != null) {
            ps.setTime(7, Time.valueOf(request.getStartTime()));
        } else {
            ps.setNull(7, Types.TIME);
        }
        if (request.getEndTime() != null) {
            ps.setTime(8, Time.valueOf(request.getEndTime()));
        } else {
            ps.setNull(8, Types.TIME);
        }
        ps.setString(9, request.getStatus().name());
    }

    private List<PermissionRequest> queryList(String sql, StatementConfigurer configurer) {
        List<PermissionRequest> requests = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            configurer.configure(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapRow(rs));
                }
            }
            return requests;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to query permission requests.", ex);
            throw new DatabaseException("Unable to retrieve permission requests.", ex);
        }
    }

    private PermissionRequest mapRow(ResultSet rs) throws SQLException {
        PermissionRequest req = new PermissionRequest();
        req.setRequestId(rs.getInt("request_id"));
        req.setSocietyId(rs.getInt("society_id"));
        req.setSubmittedBy(rs.getInt("submitted_by"));
        req.setTitle(rs.getString("title"));
        req.setDescription(rs.getString("description"));

        Date eventDate = rs.getDate("event_date");
        req.setEventDate(eventDate != null ? eventDate.toLocalDate() : null);

        Time startTime = rs.getTime("start_time");
        req.setStartTime(startTime != null ? startTime.toLocalTime() : null);

        Time endTime = rs.getTime("end_time");
        req.setEndTime(endTime != null ? endTime.toLocalTime() : null);

        req.setStatus(RequestStatus.valueOf(rs.getString("status")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        req.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        req.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

        req.setSocietyName(rs.getString("society_name"));
        req.setSubmittedByName(rs.getString("submitted_by_name"));

        PermissionType type = new PermissionType(
                rs.getInt("type_id"),
                rs.getString("type_name"),
                rs.getString("type_description"),
                rs.getBoolean("is_active")
        );
        req.setPermissionType(type);
        return req;
    }

    @FunctionalInterface
    private interface StatementConfigurer {
        void configure(PreparedStatement ps) throws SQLException;
    }
}
