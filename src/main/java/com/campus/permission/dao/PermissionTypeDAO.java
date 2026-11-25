package com.campus.permission.dao;

import com.campus.permission.model.PermissionType;

import java.util.List;

/**
 * Data Access Object for database-driven permission categories.
 */
public interface PermissionTypeDAO extends BaseDAO<PermissionType> {

    List<PermissionType> findAllActive();
}
