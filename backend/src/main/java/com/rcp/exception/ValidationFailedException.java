package com.rcp.exception;

import org.springframework.http.HttpStatus;

// Generic 400 for input that fails a business validation rule that Bean Validation can't express
// (e.g. "start date must not be in the past", which depends on "now").
public class ValidationFailedException extends BusinessException {
    public ValidationFailedException(String message) {
        super("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
    }
}
