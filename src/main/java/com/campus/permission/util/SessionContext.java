package com.campus.permission.util;

import com.campus.permission.exception.UnauthorizedActionException;
import com.campus.permission.model.User;

/**
 * Holds the currently authenticated user for the active desktop session.
 */
public final class SessionContext {

    private static User currentUser;

    private SessionContext() {
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static User requireCurrentUser() {
        if (currentUser == null) {
            throw new UnauthorizedActionException("No authenticated user in session. Please log in.");
        }
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void clear() {
        currentUser = null;
    }
}
