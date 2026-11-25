package com.campus.permission.dao;

import com.campus.permission.enums.UserRole;
import com.campus.permission.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for application users.
 */
public interface UserDAO extends BaseDAO<User> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(UserRole role);

    List<User> findBySociety(int societyId);
}
