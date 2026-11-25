package com.campus.permission.service;

import com.campus.permission.dao.UserDAO;
import com.campus.permission.enums.UserRole;
import com.campus.permission.exception.ValidationException;
import com.campus.permission.model.User;
import com.campus.permission.util.PasswordUtil;
import com.campus.permission.util.SessionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        SessionContext.clear();
        sampleUser = new User(
                1,
                "Aarav Singh",
                "aarav.ccs@thapar.edu",
                PasswordUtil.hashPassword("Password@123"),
                UserRole.CLUB_REP,
                1
        );

        UserDAO stubUserDAO = new UserDAO() {
            @Override
            public Optional<User> findByEmail(String email) {
                return sampleUser.getEmail().equalsIgnoreCase(email)
                        ? Optional.of(sampleUser)
                        : Optional.empty();
            }

            @Override
            public List<User> findByRole(UserRole role) {
                return List.of();
            }

            @Override
            public List<User> findBySociety(int societyId) {
                return List.of();
            }

            @Override
            public User save(User entity) {
                return entity;
            }

            @Override
            public Optional<User> findById(int id) {
                return id == 1 ? Optional.of(sampleUser) : Optional.empty();
            }

            @Override
            public List<User> findAll() {
                return List.of(sampleUser);
            }
        };

        authService = new AuthService(stubUserDAO);
    }

    @AfterEach
    void tearDown() {
        SessionContext.clear();
    }

    @Test
    void validLoginSetsSessionAndReturnsUser() {
        User loggedIn = authService.login("Aarav.CCS@thapar.edu", "Password@123");

        assertNotNull(loggedIn);
        assertEquals(1, loggedIn.getUserId());
        assertEquals(UserRole.CLUB_REP, loggedIn.getRole());
        assertTrue(SessionContext.isLoggedIn());
        assertEquals(loggedIn, SessionContext.getCurrentUser());
    }

    @Test
    void invalidPasswordThrowsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.login("aarav.ccs@thapar.edu", "WrongPassword"));

        assertEquals("Invalid login credentials.", ex.getMessage());
        assertFalse(SessionContext.isLoggedIn());
    }

    @Test
    void unknownEmailThrowsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> authService.login("nobody@thapar.edu", "Password@123"));

        assertEquals("Invalid login credentials.", ex.getMessage());
        assertFalse(SessionContext.isLoggedIn());
    }

    @Test
    void blankPasswordOrInvalidEmailThrowsValidationException() {
        assertThrows(ValidationException.class, () -> authService.login("not-an-email", "Password@123"));
        assertThrows(ValidationException.class, () -> authService.login("aarav.ccs@thapar.edu", "   "));
    }

    @Test
    void logoutClearsSession() {
        authService.login("aarav.ccs@thapar.edu", "Password@123");
        assertTrue(SessionContext.isLoggedIn());

        authService.logout();
        assertFalse(SessionContext.isLoggedIn());
        assertNull(SessionContext.getCurrentUser());
    }
}
