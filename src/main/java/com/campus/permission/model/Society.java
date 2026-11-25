package com.campus.permission.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a registered college club or society.
 */
public class Society implements Serializable {

    private static final long serialVersionUID = 1L;

    private int societyId;
    private String societyName;
    private String description;
    private int facultyAdvisorId;

    public Society() {
    }

    public Society(int societyId, String societyName, String description, int facultyAdvisorId) {
        this.societyId = societyId;
        this.societyName = societyName;
        this.description = description;
        this.facultyAdvisorId = facultyAdvisorId;
    }

    public int getSocietyId() {
        return societyId;
    }

    public void setSocietyId(int societyId) {
        this.societyId = societyId;
    }

    public String getSocietyName() {
        return societyName;
    }

    public void setSocietyName(String societyName) {
        this.societyName = societyName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getFacultyAdvisorId() {
        return facultyAdvisorId;
    }

    public void setFacultyAdvisorId(int facultyAdvisorId) {
        this.facultyAdvisorId = facultyAdvisorId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Society other)) return false;
        return societyId == other.societyId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(societyId);
    }

    @Override
    public String toString() {
        return societyName != null ? societyName : ("Society #" + societyId);
    }
}
