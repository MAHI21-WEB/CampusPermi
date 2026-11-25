package com.campus.permission.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents metadata for a supporting document attached to a permission request.
 */
public class Attachment implements Serializable {

    private static final long serialVersionUID = 1L;

    private int attachmentId;
    private int requestId;
    private String fileName;
    private String filePath;
    private LocalDateTime uploadedAt;

    public Attachment() {
    }

    public Attachment(int attachmentId, int requestId, String fileName, String filePath, LocalDateTime uploadedAt) {
        this.attachmentId = attachmentId;
        this.requestId = requestId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.uploadedAt = uploadedAt;
    }

    public int getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(int attachmentId) {
        this.attachmentId = attachmentId;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Attachment other)) return false;
        return attachmentId == other.attachmentId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attachmentId);
    }

    @Override
    public String toString() {
        return fileName != null ? fileName : ("Attachment #" + attachmentId);
    }
}
