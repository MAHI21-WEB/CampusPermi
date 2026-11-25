package com.campus.permission.dao.impl;

import com.campus.permission.dao.ApprovalDAO;
import com.campus.permission.enums.ApprovalLevel;
import com.campus.permission.enums.Decision;
import com.campus.permission.exception.DatabaseException;
import com.campus.permission.model.Approval;
import com.campus.permission.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of {@link ApprovalDAO}.
 */
public class ApprovalDAOImpl implements ApprovalDAO {

    private static final Logger LOGGER = Logger.getLogger(ApprovalDAOImpl.class.getName());

    private static final String BASE_SELECT =
            "SELECT a.approval_id, a.request_id, a.approver_id, u.name AS approver_name, " +
            "       a.approval_level, a.decision, a.remarks, a.decision_date " +
            "FROM Approvals a " +
            "JOIN Users u ON a.approver_id = u.user_id ";

    @Override
    public Approval save(Approval approval) {
        try (Connection conn = DBConnection.getConnection()) {
            return save(conn, approval);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to save approval record.", ex);
            throw new DatabaseException("Unable to save approval record.", ex);
        }
    }

    @Override
    public Approval save(Connection connection, Approval approval) {
        String sql = "INSERT INTO Approvals (request_id, approver_id, approval_level, decision, remarks) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, approval.getRequestId());
            ps.setInt(2, approval.getApproverId());
            ps.setString(3, approval.getApprovalLevel().name());
            ps.setString(4, approval.getDecision().name());
            ps.setString(5, approval.getRemarks());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    approval.setApprovalId(keys.getInt(1));
                }
            }
            return approval;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to insert approval record.", ex);
            throw new DatabaseException("Unable to record approval decision.", ex);
        }
    }

    @Override
    public Optional<Approval> findById(int id) {
        String sql = BASE_SELECT + "WHERE a.approval_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to find approval by id: " + id, ex);
            throw new DatabaseException("Unable to retrieve approval record.", ex);
        }
    }

    @Override
    public List<Approval> findAll() {
        return queryList(BASE_SELECT + "ORDER BY a.decision_date DESC", ps -> { });
    }

    @Override
    public List<Approval> findByRequestId(int requestId) {
        String sql = BASE_SELECT + "WHERE a.request_id = ? ORDER BY a.decision_date ASC";
        return queryList(sql, ps -> ps.setInt(1, requestId));
    }

    @Override
    public List<Approval> findByApproverId(int approverId) {
        String sql = BASE_SELECT + "WHERE a.approver_id = ? ORDER BY a.decision_date DESC";
        return queryList(sql, ps -> ps.setInt(1, approverId));
    }

    @Override
    public List<Approval> findByApprovalLevel(ApprovalLevel level) {
        String sql = BASE_SELECT + "WHERE a.approval_level = ? ORDER BY a.decision_date DESC";
        return queryList(sql, ps -> ps.setString(1, level.name()));
    }

    private List<Approval> queryList(String sql, StatementConfigurer configurer) {
        List<Approval> approvals = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            configurer.configure(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    approvals.add(mapRow(rs));
                }
            }
            return approvals;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to query approvals.", ex);
            throw new DatabaseException("Unable to retrieve approval history.", ex);
        }
    }

    private Approval mapRow(ResultSet rs) throws SQLException {
        Approval approval = new Approval();
        approval.setApprovalId(rs.getInt("approval_id"));
        approval.setRequestId(rs.getInt("request_id"));
        approval.setApproverId(rs.getInt("approver_id"));
        approval.setApproverName(rs.getString("approver_name"));
        approval.setApprovalLevel(ApprovalLevel.valueOf(rs.getString("approval_level")));
        approval.setDecision(Decision.valueOf(rs.getString("decision")));
        approval.setRemarks(rs.getString("remarks"));
        Timestamp decisionDate = rs.getTimestamp("decision_date");
        approval.setDecisionDate(decisionDate != null ? decisionDate.toLocalDateTime() : null);
        return approval;
    }

    @FunctionalInterface
    private interface StatementConfigurer {
        void configure(PreparedStatement ps) throws SQLException;
    }
}
