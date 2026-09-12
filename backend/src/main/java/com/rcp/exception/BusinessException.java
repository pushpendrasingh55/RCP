package com.rcp.exception;

import org.springframework.http.HttpStatus;

// Base type for every "expected" failure: bad input, illegal workflow transition, tenant mismatch, etc.
// Each instance carries the HTTP status and the machine-readable error code to put in the response body,
// so GlobalExceptionHandler never has to guess how to render it.
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public BusinessException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
