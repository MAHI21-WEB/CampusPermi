package com.campus.permission.dao;

import com.campus.permission.model.Society;

import java.util.List;

/**
 * Data Access Object for registered college clubs and societies.
 */
public interface SocietyDAO extends BaseDAO<Society> {

    List<Society> findByFacultyAdvisor(int facultyAdvisorId);
}
