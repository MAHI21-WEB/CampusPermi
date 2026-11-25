package com.campus.permission.exception;

/**
 * Thrown when a user attempts an action not permitted for their role or ownership.
 */
public class UnauthorizedActionException extends RuntimeException {

    public UnauthorizedActionException(String message) {
        super(message);
    }

    public UnauthorizedActionException(String message, Throwable cause) {
        super(message, cause);
    }
}
