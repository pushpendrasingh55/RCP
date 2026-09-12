package com.rcp.domain;

// The three actor roles defined by the spec. A user's RequestInfo.userInfo.roles list
// is mapped onto these values; anything not recognised is ignored (treated as "no role").
public enum Role {
    APPLICANT,
    VERIFIER,
    APPROVER
}
