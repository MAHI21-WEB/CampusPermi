package com.campus.permission.config;

import com.campus.permission.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads application configuration from properties files without hardcoding credentials.
 */
public final class AppConfig {

    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static final String CLASSPATH_CONFIG = "/config/application.properties";
    private static final Path LOCAL_CONFIG = Path.of("application.properties");
    private static final Properties PROPERTIES = new Properties();

    static {
        loadProperties();
    }

    private AppConfig() {
    }

    private static void loadProperties() {
        try (InputStream stream = AppConfig.class.getResourceAsStream(CLASSPATH_CONFIG)) {
            if (stream != null) {
                PROPERTIES.load(stream);
                LOGGER.info("Loaded configuration from classpath " + CLASSPATH_CONFIG);
                return;
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Failed to read classpath configuration.", ex);
        }

        if (Files.exists(LOCAL_CONFIG)) {
            try (InputStream fileStream = Files.newInputStream(LOCAL_CONFIG)) {
                PROPERTIES.load(fileStream);
                LOGGER.info("Loaded configuration from local application.properties");
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Failed to read local application.properties.", ex);
            }
        } else {
            LOGGER.warning("No application.properties found; database operations will require configuration.");
        }
    }

    public static String getDbUrl() {
        return getRequiredProperty("db.url");
    }

    public static String getDbUsername() {
        return getRequiredProperty("db.username");
    }

    public static String getDbPassword() {
        String value = PROPERTIES.getProperty("db.password");
        if (value == null) {
            throw new DatabaseException("Database configuration is incomplete. Check application.properties.");
        }
        return value;
    }

    public static String getUploadsDir() {
        return PROPERTIES.getProperty("app.uploads.dir", "uploads");
    }

    public static String getBackupsDir() {
        return PROPERTIES.getProperty("app.backups.dir", "backups");
    }

    private static String getRequiredProperty(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new DatabaseException("Database configuration is incomplete. Missing property: " + key);
        }
        return value.trim();
    }
}
