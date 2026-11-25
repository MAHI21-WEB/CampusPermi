package com.campus.permission.model;

import com.campus.permission.enums.ApprovalLevel;
import com.campus.permission.enums.Decision;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents one immutable approval or rejection decision in a request's history.
 */
public class Approval implements Serializable {

    private static final long serialVersionUID = 1L;

    private int approvalId;
    private int requestId;
    private int approverId;
    private String approverName;
    private ApprovalLevel approvalLevel;
    private Decision decision;
    private String remarks;
    private LocalDateTime decisionDate;

    public Approval() {
    }

    public int getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(int approvalId) {
        this.approvalId = approvalId;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getApproverId() {
        return approverId;
    }

    public void setApproverId(int approverId) {
        this.approverId = approverId;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public ApprovalLevel getApprovalLevel() {
        return approvalLevel;
    }

    public void setApprovalLevel(ApprovalLevel approvalLevel) {
        this.approvalLevel = approvalLevel;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getDecisionDate() {
        return decisionDate;
    }

    public void setDecisionDate(LocalDateTime decisionDate) {
        this.decisionDate = decisionDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Approval other)) return false;
        return approvalId == other.approvalId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(approvalId);
    }

    @Override
    public String toString() {
        return "Approval{" +
                "approvalId=" + approvalId +
                ", requestId=" + requestId +
                ", approvalLevel=" + approvalLevel +
                ", decision=" + decision +
                '}';
    }
}
