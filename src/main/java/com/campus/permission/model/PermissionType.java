package com.campus.permission.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a database-driven permission category.
 */
public class PermissionType implements Serializable {

    private static final long serialVersionUID = 1L;

    private int typeId;
    private String typeName;
    private String description;
    private boolean active;

    public PermissionType() {
    }

    public PermissionType(int typeId, String typeName, String description, boolean active) {
        this.typeId = typeId;
        this.typeName = typeName;
        this.description = description;
        this.active = active;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PermissionType other)) return false;
        return typeId == other.typeId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeId);
    }

    @Override
    public String toString() {
        return typeName != null ? typeName : ("PermissionType #" + typeId);
    }
}
