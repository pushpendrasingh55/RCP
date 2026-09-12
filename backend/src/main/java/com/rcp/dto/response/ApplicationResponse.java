package com.rcp.dto.response;

import com.rcp.domain.ApplicantType;
import com.rcp.domain.ApplicationStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

// The full representation of an application returned by _create, _action and (per item) _search.
// Bundles the persisted fee breakdown, current status, and - crucially - its transition history,
// so a single response is enough for the frontend to render both the detail view and its timeline.
@Getter
@Builder
public class ApplicationResponse {
    private String applicationNumber;
    private String tenantId;
    private String roadType;
    private BigDecimal lengthInMeters;
    private BigDecimal widthInMeters;
    private BigDecimal areaInSqm;
    private Integer durationInDays;
    private ApplicantType applicantType;
    private LocalDate proposedStartDate;
    private LocalDate applicationDate;

    private BigDecimal restorationCharge;
    private BigDecimal permissionFee;
    private BigDecimal urgencySurcharge;
    private BigDecimal securityDeposit;
    private BigDecimal totalAmount;

    private ApplicationStatus status;
    // The actions the CURRENT caller (given their role and this application's status) may legally take next.
    // The officer UI must build its action buttons from this list, not from a hardcoded switch.
    private List<String> availableActions;

    private String applicantMobileNumber;

    private Instant createdTime;
    private Instant lastModifiedTime;

    private List<TransitionHistoryEntry> history;
}
