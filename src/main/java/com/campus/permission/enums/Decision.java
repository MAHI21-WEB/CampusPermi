package com.campus.permission.enums;

/**
 * Decision recorded by an approving authority.
 */
public enum Decision {
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayName;

    Decision(String displayName) {
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
