package com.campus.permission.dao.impl;

import com.campus.permission.dao.AttachmentDAO;
import com.campus.permission.exception.DatabaseException;
import com.campus.permission.model.Attachment;
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
 * JDBC implementation of {@link AttachmentDAO}.
 */
public class AttachmentDAOImpl implements AttachmentDAO {

    private static final Logger LOGGER = Logger.getLogger(AttachmentDAOImpl.class.getName());

    private static final String BASE_SELECT =
            "SELECT attachment_id, request_id, file_name, file_path, uploaded_at FROM Attachments ";

    @Override
    public Attachment save(Attachment attachment) {
        String sql = "INSERT INTO Attachments (request_id, file_name, file_path) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, attachment.getRequestId());
            ps.setString(2, attachment.getFileName());
            ps.setString(3, attachment.getFilePath());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    attachment.setAttachmentId(keys.getInt(1));
                }
            }
            return attachment;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to save attachment metadata.", ex);
            throw new DatabaseException("Unable to save attachment metadata.", ex);
        }
    }

    @Override
    public Optional<Attachment> findById(int id) {
        String sql = BASE_SELECT + "WHERE attachment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to find attachment by id: " + id, ex);
            throw new DatabaseException("Unable to retrieve attachment.", ex);
        }
    }

    @Override
    public List<Attachment> findAll() {
        String sql = BASE_SELECT + "ORDER BY uploaded_at DESC";
        List<Attachment> attachments = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                attachments.add(mapRow(rs));
            }
            return attachments;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to list attachments.", ex);
            throw new DatabaseException("Unable to retrieve attachments.", ex);
        }
    }

    @Override
    public List<Attachment> findByRequestId(int requestId) {
        String sql = BASE_SELECT + "WHERE request_id = ? ORDER BY uploaded_at ASC";
        List<Attachment> attachments = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    attachments.add(mapRow(rs));
                }
            }
            return attachments;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to find attachments for request: " + requestId, ex);
            throw new DatabaseException("Unable to retrieve request attachments.", ex);
        }
    }

    @Override
    public void deleteById(int attachmentId) {
        String sql = "DELETE FROM Attachments WHERE attachment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attachmentId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to delete attachment: " + attachmentId, ex);
            throw new DatabaseException("Unable to delete attachment.", ex);
        }
    }

    private Attachment mapRow(ResultSet rs) throws SQLException {
        Timestamp uploadedAt = rs.getTimestamp("uploaded_at");
        return new Attachment(
                rs.getInt("attachment_id"),
                rs.getInt("request_id"),
                rs.getString("file_name"),
                rs.getString("file_path"),
                uploadedAt != null ? uploadedAt.toLocalDateTime() : null
        );
    }
}
