package com.campus.permission.util;

import com.campus.permission.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * Common input validation helpers used across services and controllers.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private ValidationUtil() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String requireNonBlank(String value, String fieldName) {
        if (isBlank(value)) {
            throw new ValidationException(fieldName + " is required.");
        }
        return value.trim();
    }

    public static String validateEmail(String email) {
        String trimmed = requireNonBlank(email, "Email");
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Please enter a valid email address.");
        }
        return trimmed.toLowerCase();
    }
}
