package com.campus.permission.dao.impl;

import com.campus.permission.dao.UserDAO;
import com.campus.permission.enums.UserRole;
import com.campus.permission.exception.DatabaseException;
import com.campus.permission.model.User;
import com.campus.permission.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of {@link UserDAO}.
 */
public class UserDAOImpl implements UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAOImpl.class.getName());

    private static final String BASE_SELECT =
            "SELECT user_id, name, email, password_hash, role, society_id FROM Users ";

    @Override
    public User save(User user) {
        String sql = "INSERT INTO Users (name, email, password_hash, role, society_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            if (user.getSocietyId() != null) {
                ps.setInt(5, user.getSocietyId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setUserId(keys.getInt(1));
                }
            }
            return user;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to save user.", ex);
            throw new DatabaseException("Unable to save user record.", ex);
        }
    }

    @Override
    public Optional<User> findById(int id) {
        String sql = BASE_SELECT + "WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to find user by id: " + id, ex);
            throw new DatabaseException("Unable to retrieve user record.", ex);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = BASE_SELECT + "WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to find user by email.", ex);
            throw new DatabaseException("Unable to retrieve user record.", ex);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = BASE_SELECT + "ORDER BY name";
        return queryList(sql, ps -> { });
    }

    @Override
    public List<User> findByRole(UserRole role) {
        String sql = BASE_SELECT + "WHERE role = ? ORDER BY name";
        return queryList(sql, ps -> ps.setString(1, role.name()));
    }

    @Override
    public List<User> findBySociety(int societyId) {
        String sql = BASE_SELECT + "WHERE society_id = ? ORDER BY name";
        return queryList(sql, ps -> ps.setInt(1, societyId));
    }

    private List<User> queryList(String sql, StatementConfigurer configurer) {
        List<User> users = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            configurer.configure(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
            }
            return users;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to query user list.", ex);
            throw new DatabaseException("Unable to retrieve users.", ex);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(UserRole.valueOf(rs.getString("role")));
        int societyId = rs.getInt("society_id");
        user.setSocietyId(rs.wasNull() ? null : societyId);
        return user;
    }

    @FunctionalInterface
    private interface StatementConfigurer {
        void configure(PreparedStatement ps) throws SQLException;
    }
}
