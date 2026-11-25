package com.campus.permission.util;

import com.campus.permission.config.AppConfig;
import com.campus.permission.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC connection factory for the MySQL campus_permission_db database.
 */
public final class DBConnection {

    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());

    private DBConnection() {
    }

    /**
     * Opens a new JDBC connection using configured credentials.
     *
     * @return an open {@link Connection}
     */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                    AppConfig.getDbUrl(),
                    AppConfig.getDbUsername(),
                    AppConfig.getDbPassword()
            );
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Unable to establish MySQL database connection.", ex);
            throw new DatabaseException("Database connection unavailable.", ex);
        }
    }
}
