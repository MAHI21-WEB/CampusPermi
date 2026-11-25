package com.campus.permission.dao.impl;

import com.campus.permission.dao.SocietyDAO;
import com.campus.permission.exception.DatabaseException;
import com.campus.permission.model.Society;
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
 * JDBC implementation of {@link SocietyDAO}.
 */
public class SocietyDAOImpl implements SocietyDAO {

    private static final Logger LOGGER = Logger.getLogger(SocietyDAOImpl.class.getName());

    private static final String BASE_SELECT =
            "SELECT society_id, society_name, description, faculty_advisor_id FROM Societies ";

    @Override
    public Society save(Society society) {
        String sql = "INSERT INTO Societies (society_name, description, faculty_advisor_id) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, society.getSocietyName());
            ps.setString(2, society.getDescription());
            ps.setInt(3, society.getFacultyAdvisorId());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    society.setSocietyId(keys.getInt(1));
                }
            }
            return society;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to save society.", ex);
            throw new DatabaseException("Unable to save society record.", ex);
        }
    }

    @Override
    public Optional<Society> findById(int id) {
        String sql = BASE_SELECT + "WHERE society_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to find society by id: " + id, ex);
            throw new DatabaseException("Unable to retrieve society record.", ex);
        }
    }

    @Override
    public List<Society> findAll() {
        String sql = BASE_SELECT + "ORDER BY society_name";
        List<Society> societies = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                societies.add(mapRow(rs));
            }
            return societies;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to list societies.", ex);
            throw new DatabaseException("Unable to retrieve societies.", ex);
        }
    }

    @Override
    public List<Society> findByFacultyAdvisor(int facultyAdvisorId) {
        String sql = BASE_SELECT + "WHERE faculty_advisor_id = ? ORDER BY society_name";
        List<Society> societies = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, facultyAdvisorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    societies.add(mapRow(rs));
                }
            }
            return societies;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to find societies for advisor: " + facultyAdvisorId, ex);
            throw new DatabaseException("Unable to retrieve assigned societies.", ex);
        }
    }

    private Society mapRow(ResultSet rs) throws SQLException {
        return new Society(
                rs.getInt("society_id"),
                rs.getString("society_name"),
                rs.getString("description"),
                rs.getInt("faculty_advisor_id")
        );
    }
}
