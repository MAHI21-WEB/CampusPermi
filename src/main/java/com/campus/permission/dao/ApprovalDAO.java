package com.campus.permission.dao;

import com.campus.permission.enums.ApprovalLevel;
import com.campus.permission.model.Approval;

import java.sql.Connection;
import java.util.List;

/**
 * Data Access Object for immutable approval decision history.
 */
public interface ApprovalDAO extends BaseDAO<Approval> {

    Approval save(Connection connection, Approval approval);

    List<Approval> findByRequestId(int requestId);

    List<Approval> findByApproverId(int approverId);

    List<Approval> findByApprovalLevel(ApprovalLevel level);
}
