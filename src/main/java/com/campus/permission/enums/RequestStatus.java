package com.campus.permission.enums;

/**
 * Lifecycle states of a permission request.
 */
public enum RequestStatus {
    DRAFT("Draft"),
    PENDING_FACULTY("Pending Faculty Advisor"),
    PENDING_DOSA("Pending DOSA Review"),
    PENDING_ASSOCIATE_DEAN("Pending Associate Dean"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
