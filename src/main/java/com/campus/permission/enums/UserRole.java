package com.campus.permission.enums;

/**
 * Application user roles matching the four-tier CPMS access model.
 */
public enum UserRole {
    CLUB_REP("Club/Society Representative"),
    FACULTY_ADVISOR("Faculty Advisor"),
    DOSA_REP("DOSA Office Representative"),
    ASSOCIATE_DEAN("Associate Dean");

    private final String displayName;

    UserRole(String displayName) {
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
