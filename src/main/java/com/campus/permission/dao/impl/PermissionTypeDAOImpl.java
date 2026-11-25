package com.campus.permission.dao.impl;

import com.campus.permission.dao.PermissionTypeDAO;
import com.campus.permission.exception.DatabaseException;
import com.campus.permission.model.PermissionType;
import com.campus.permission.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of {@link PermissionTypeDAO}.
 */
public class PermissionTypeDAOImpl implements PermissionTypeDAO {

    private static final Logger LOGGER = Logger.getLogger(PermissionTypeDAOImpl.class.getName());

    private static final String BASE_SELECT =
            "SELECT type_id, type_name, description, is_active FROM Permission_Types ";

    @Override
    public PermissionType save(PermissionType type) {
        String sql = "INSERT INTO Permission_Types (type_name, description, is_active) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, type.getTypeName());
            ps.setString(2, type.getDescription());
            ps.setBoolean(3, type.isActive());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    type.setTypeId(keys.getInt(1));
                }
            }
            return type;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to save permission type.", ex);
            throw new DatabaseException("Unable to save permission category.", ex);
        }
    }

    @Override
    public Optional<PermissionType> findById(int id) {
        String sql = BASE_SELECT + "WHERE type_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to find permission type by id: " + id, ex);
            throw new DatabaseException("Unable to retrieve permission category.", ex);
        }
    }

    @Override
    public List<PermissionType> findAll() {
        return queryList(BASE_SELECT + "ORDER BY type_id");
    }

    @Override
    public List<PermissionType> findAllActive() {
        return queryList(BASE_SELECT + "WHERE is_active = TRUE ORDER BY type_id");
    }

    private List<PermissionType> queryList(String sql) {
        List<PermissionType> types = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                types.add(mapRow(rs));
            }
            return types;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve permission categories.", ex);
            throw new DatabaseException("Unable to retrieve permission categories.", ex);
        }
    }

    private PermissionType mapRow(ResultSet rs) throws SQLException {
        return new PermissionType(
                rs.getInt("type_id"),
                rs.getString("type_name"),
                rs.getString("description"),
                rs.getBoolean("is_active")
        );
    }
}
