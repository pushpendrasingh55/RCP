package com.rcp.exception;

import com.rcp.dto.ErrorDetail;
import com.rcp.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

// Every controller in this application funnels its exceptions through here, so that:
//   (a) validation/business errors always come back as 4xx with the required { ResponseInfo, Errors } shape, and
//   (b) truly unexpected errors are logged and returned as a generic 500 - never a raw stack trace, and
//       never silently swallowed (no bare "catch (Exception e) {}" anywhere in this codebase).
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Our own hierarchy of "expected" failures. Each one already knows its HTTP status and error code.
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.info("Business exception: {} - {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
    }

    // Bean Validation failures (@NotNull, @Positive, etc. on request DTOs) -> 400 with one error per field.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ErrorDetail("VALIDATION_ERROR",
                        fieldError.getField() + ": " + fieldError.getDefaultMessage()))
                .toList();
        if (errors.isEmpty()) {
            errors = List.of(new ErrorDetail("VALIDATION_ERROR", "Request failed validation"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(errors));
    }

    // Belt-and-braces: Spring Data can throw this directly in some code paths in addition to the
    // JPA-specific subtype below. Both are treated as a 409 Conflict, never a 500.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.info("Optimistic locking failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("CONCURRENT_UPDATE",
                        "This application was modified by someone else at the same time. Please reload and try again."));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleObjectOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return handleOptimisticLock(ex);
    }

    // Last line of defence. Anything not already handled above is logged in full (server-side only)
    // and reported to the caller as a generic, non-leaky 500.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred. Please try again later."));
    }
}
