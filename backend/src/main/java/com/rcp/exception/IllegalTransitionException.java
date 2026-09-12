package com.rcp.exception;

import org.springframework.http.HttpStatus;

// Thrown by the workflow service when the requested action is not a legal transition from the
// application's current state, or the actor's role is not permitted to perform it.
public class IllegalTransitionException extends BusinessException {
    public IllegalTransitionException(String message) {
        super("ILLEGAL_TRANSITION", message, HttpStatus.BAD_REQUEST);
    }
}
