package com.campus.permission.enums;

/**
 * Sequential approval authority levels.
 */
public enum ApprovalLevel {
    FACULTY("Faculty Advisor"),
    DOSA("DOSA Representative"),
    ASSOCIATE_DEAN("Associate Dean");

    private final String displayName;

    ApprovalLevel(String displayName) {
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
