package com.rcp.repository;

import com.rcp.domain.Application;
import com.rcp.domain.ApplicationStatus;
import org.springframework.data.jpa.domain.Specification;

// Builds the WHERE clause for POST /rcp/v1/_search as a composable set of predicates.
// tenantId(...) is ALWAYS included by ApplicationService, unconditionally, regardless of what
// filters the caller supplied - see the big comment in ApplicationService.search().
public final class ApplicationSpecifications {

    private ApplicationSpecifications() {
    }

    public static Specification<Application> tenantId(String tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<Application> applicationNumber(String applicationNumber) {
        if (applicationNumber == null || applicationNumber.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("applicationNumber"), applicationNumber);
    }

    public static Specification<Application> status(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        ApplicationStatus parsed;
        try {
            parsed = ApplicationStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            // An unrecognised status filter should simply match nothing, not blow up the whole search.
            return (root, query, cb) -> cb.disjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), parsed);
    }

    public static Specification<Application> mobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("applicantMobileNumber"), mobileNumber);
    }

    public static Specification<Application> applicantUuid(String applicantUuid) {
        if (applicantUuid == null || applicantUuid.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("applicantUuid"), applicantUuid);
    }

    public static Specification<Application> allOf(Specification<Application>... specs) {
        Specification<Application> combined = Specification.where(null);
        for (Specification<Application> spec : specs) {
            if (spec != null) {
                combined = combined.and(spec);
            }
        }
        return combined;
    }
}
