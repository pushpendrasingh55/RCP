package com.rcp.domain;

// The five lifecycle states an application can be in. The *transitions* between these states
// live in workflow configuration (see com.rcp.workflow), not here - this enum only names the states.
public enum ApplicationStatus {
    APPLIED,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    CANCELLED
}
