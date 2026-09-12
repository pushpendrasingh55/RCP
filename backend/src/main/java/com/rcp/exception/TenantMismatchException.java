package com.rcp.exception;

import org.springframework.http.HttpStatus;

// Thrown when the tenantId inside a request body disagrees with the caller's own
// RequestInfo.userInfo.tenantId. The server never trusts a client-asserted tenant.
public class TenantMismatchException extends BusinessException {
    public TenantMismatchException(String message) {
        super("TENANT_MISMATCH", message, HttpStatus.FORBIDDEN);
    }
}
