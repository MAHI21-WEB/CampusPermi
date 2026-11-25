package com.campus.permission.service;

import com.campus.permission.dao.UserDAO;
import com.campus.permission.dao.impl.UserDAOImpl;
import com.campus.permission.exception.ValidationException;
import com.campus.permission.model.User;
import com.campus.permission.util.PasswordUtil;
import com.campus.permission.util.SessionContext;
import com.campus.permission.util.ValidationUtil;

import java.util.logging.Logger;

/**
 * Handles user authentication, password verification, and session lifecycle.
 */
public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());
    private final UserDAO userDAO;

    public AuthService() {
        this(new UserDAOImpl());
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authenticates a user by email and password and initializes {@link SessionContext}.
     *
     * @param email    user email
     * @param password plain-text password
     * @return authenticated {@link User}
     */
    public User login(String email, String password) {
        String normalizedEmail = ValidationUtil.validateEmail(email);
        ValidationUtil.requireNonBlank(password, "Password");

        User user = userDAO.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    LOGGER.warning("Failed login attempt for unknown email.");
                    return new ValidationException("Invalid login credentials.");
                });

        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            LOGGER.warning("Failed login attempt due to invalid password for userId=" + user.getUserId());
            throw new ValidationException("Invalid login credentials.");
        }

        SessionContext.setCurrentUser(user);
        LOGGER.info("User logged in: userId=" + user.getUserId() + ", role=" + user.getRole());
        return user;
    }

    /**
     * Clears the current user session.
     */
    public void logout() {
        User current = SessionContext.getCurrentUser();
        if (current != null) {
            LOGGER.info("User logged out: userId=" + current.getUserId());
        }
        SessionContext.clear();
    }
}
