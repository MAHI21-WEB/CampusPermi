package com.campus.permission.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic base Data Access Object contract for common persistence operations.
 *
 * @param <T> model entity type
 */
public interface BaseDAO<T> {

    T save(T entity);

    Optional<T> findById(int id);

    List<T> findAll();
}
