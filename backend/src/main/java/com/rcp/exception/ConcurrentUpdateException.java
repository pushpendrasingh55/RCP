package com.rcp.exception;

import org.springframework.http.HttpStatus;

// Thrown when an optimistic-locking failure is detected (two actors raced on the same application).
// Mapped to 409 Conflict - the caller should reload the application and retry if still applicable.
public class ConcurrentUpdateException extends BusinessException {
    public ConcurrentUpdateException(String message) {
        super("CONCURRENT_UPDATE", message, HttpStatus.CONFLICT);
    }
}
