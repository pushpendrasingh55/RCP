package com.rcp.validation;

import com.rcp.config.RcpProperties;
import com.rcp.dto.RequestInfo;
import com.rcp.exception.TenantMismatchException;
import com.rcp.exception.ValidationFailedException;
import org.springframework.stereotype.Component;

// Reconciles the tenant identity asserted inside a request body (Calculation.tenantId) against the
// caller's own, server-side-authoritative tenant identity (RequestInfo.userInfo.tenantId).
//
// We NEVER use a client-sent tenantId on its own to decide which tenant's data to touch - if the two
// disagree, the request is rejected outright rather than "helpfully" picking one of them.
@Component
public class TenantValidator {

    private final RcpProperties rcpProperties;

    public TenantValidator(RcpProperties rcpProperties) {
        this.rcpProperties = rcpProperties;
    }

    public void assertConsistent(RequestInfo requestInfo, String bodyTenantId) {
        String callerTenantId = requestInfo.getUserInfo().getTenantId();
        assertKnownTenant(callerTenantId);
        if (bodyTenantId != null && !bodyTenantId.equals(callerTenantId)) {
            throw new TenantMismatchException(
                    "Request tenantId '" + bodyTenantId + "' does not match caller's tenant '" + callerTenantId + "'");
        }
    }

    // Rejects a tenantId the deployment has never heard of (i.e. not in rcp.tenant-prefixes) as a
    // clean 400, instead of letting it surface later as a confusing failure inside number generation.
    public void assertKnownTenant(String tenantId) {
        if (!rcpProperties.getTenantPrefixes().containsKey(tenantId)) {
            throw new ValidationFailedException("Unknown tenant '" + tenantId + "'");
        }
    }
}
