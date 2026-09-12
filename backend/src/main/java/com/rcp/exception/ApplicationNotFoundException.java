package com.rcp.exception;

import org.springframework.http.HttpStatus;

// Thrown both for a genuinely-missing application number AND for an application that exists but
// belongs to a different tenant. Returning the same 404 in both cases avoids confirming to a caller
// from tenant A that a given application number exists (just in tenant B) - see README "Tenant isolation".
public class ApplicationNotFoundException extends BusinessException {
    public ApplicationNotFoundException(String applicationNumber) {
        super("APPLICATION_NOT_FOUND", "No application found with number " + applicationNumber, HttpStatus.NOT_FOUND);
    }
}
