package com.rcp.exception;

import org.springframework.http.HttpStatus;

// Thrown when a roadType code is unknown to the rate configuration, or known but inactive.
public class InvalidRoadTypeException extends BusinessException {
    public InvalidRoadTypeException(String message) {
        super("INVALID_ROAD_TYPE", message, HttpStatus.BAD_REQUEST);
    }
}
