package com.campus.permission.dao;

import com.campus.permission.model.Attachment;

import java.util.List;

/**
 * Data Access Object for supporting request attachments.
 */
public interface AttachmentDAO extends BaseDAO<Attachment> {

    List<Attachment> findByRequestId(int requestId);

    void deleteById(int attachmentId);
}
