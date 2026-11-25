package com.campus.permission.service;

import com.campus.permission.exception.ValidationException;
import com.campus.permission.model.PermissionRequest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides object serialization/deserialization backups and character-stream summaries
 * for permission requests.
 */
public class BackupService {

    private static final Logger LOGGER = Logger.getLogger(BackupService.class.getName());

    /**
     * Serializes a list of {@link PermissionRequest} objects to a binary backup file.
     */
    public Path exportRequestsBackup(List<PermissionRequest> requests, Path targetFile) {
        if (targetFile == null) {
            throw new ValidationException("Backup destination file is required.");
        }
        try {
            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }
            ArrayList<PermissionRequest> serializableCopy = new ArrayList<>(
                    requests != null ? requests : List.of()
            );
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(targetFile))) {
                oos.writeObject(serializableCopy);
            }
            LOGGER.info("Exported " + serializableCopy.size() + " requests to backup: " + targetFile.getFileName());
            return targetFile;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to export serialized backup.", ex);
            throw new ValidationException("Unable to export backup file: " + ex.getMessage(), ex);
        }
    }

    /**
     * Deserializes a previously exported {@link PermissionRequest} backup file.
     */
    @SuppressWarnings("unchecked")
    public List<PermissionRequest> restoreRequestsBackup(Path sourceFile) {
        if (sourceFile == null || !Files.exists(sourceFile)) {
            throw new ValidationException("Backup file does not exist.");
        }
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(sourceFile))) {
            Object obj = ois.readObject();
            if (!(obj instanceof List<?> list)) {
                throw new ValidationException("Invalid backup file format.");
            }
            List<PermissionRequest> restored = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof PermissionRequest req) {
                    restored.add(req);
                }
            }
            LOGGER.info("Restored " + restored.size() + " requests from backup: " + sourceFile.getFileName());
            return restored;
        } catch (IOException | ClassNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "Failed to restore serialized backup.", ex);
            throw new ValidationException("Unable to read backup file.", ex);
        }
    }

    /**
     * Exports a human-readable text summary using a character stream ({@link BufferedWriter}).
     */
    public Path exportSummaryText(List<PermissionRequest> requests, Path targetFile) {
        if (targetFile == null) {
            throw new ValidationException("Summary destination file is required.");
        }
        try {
            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {
                writer.write("CPMS Permission Requests Summary");
                writer.newLine();
                writer.write("================================");
                writer.newLine();
                if (requests != null) {
                    for (PermissionRequest req : requests) {
                        writer.write(String.format("#%d | %s | %s",
                                req.getRequestId(),
                                req.getTitle(),
                                req.getStatus() != null ? req.getStatus().getDisplayName() : "N/A"));
                        writer.newLine();
                    }
                }
            }
            return targetFile;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to write text summary.", ex);
            throw new ValidationException("Unable to export summary file.", ex);
        }
    }
}
