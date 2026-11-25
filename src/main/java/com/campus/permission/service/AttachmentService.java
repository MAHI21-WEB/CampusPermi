package com.campus.permission.service;

import com.campus.permission.config.AppConfig;
import com.campus.permission.dao.AttachmentDAO;
import com.campus.permission.dao.impl.AttachmentDAOImpl;
import com.campus.permission.exception.DatabaseException;
import com.campus.permission.exception.ValidationException;
import com.campus.permission.model.Attachment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles supporting document storage via Java I/O byte streams and metadata persistence.
 */
public class AttachmentService {

    private static final Logger LOGGER = Logger.getLogger(AttachmentService.class.getName());
    private static final int BUFFER_SIZE = 8192;

    private final AttachmentDAO attachmentDAO;
    private final Path uploadsDirectory;

    public AttachmentService() {
        this(new AttachmentDAOImpl(), Path.of(AppConfig.getUploadsDir()));
    }

    public AttachmentService(AttachmentDAO attachmentDAO, Path uploadsDirectory) {
        this.attachmentDAO = attachmentDAO;
        this.uploadsDirectory = uploadsDirectory;
    }

    /**
     * Copies the selected file into the configured uploads directory using buffered byte streams
     * and saves its metadata in MySQL.
     */
    public Attachment uploadAttachment(int requestId, File sourceFile) {
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
            throw new ValidationException("Please select a valid file to attach.");
        }

        try {
            Files.createDirectories(uploadsDirectory);
            String safeName = sourceFile.getName().replaceAll("[^A-Za-z0-9._-]", "_");
            String storedFileName = "req_" + requestId + "_" + System.currentTimeMillis() + "_" + safeName;
            Path destination = uploadsDirectory.resolve(storedFileName);

            copyWithByteStreams(sourceFile, destination.toFile());

            Attachment attachment = new Attachment();
            attachment.setRequestId(requestId);
            attachment.setFileName(sourceFile.getName());
            attachment.setFilePath(destination.toAbsolutePath().toString());

            Attachment saved = attachmentDAO.save(attachment);
            LOGGER.info("Uploaded attachment '" + sourceFile.getName() + "' for requestId=" + requestId);
            return saved;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to store attachment file for requestId=" + requestId, ex);
            throw new DatabaseException("Unable to store the attachment file.", ex);
        }
    }

    public List<Attachment> getAttachmentsForRequest(int requestId) {
        return attachmentDAO.findByRequestId(requestId);
    }

    public File resolveAttachmentFile(Attachment attachment) {
        if (attachment == null || attachment.getFilePath() == null) {
            throw new ValidationException("Attachment path is missing.");
        }
        File file = new File(attachment.getFilePath());
        if (!file.exists() || !file.isFile()) {
            throw new ValidationException("Attachment file is no longer available on disk.");
        }
        return file;
    }

    private void copyWithByteStreams(File source, File destination) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(source));
             BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(destination))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }
}
