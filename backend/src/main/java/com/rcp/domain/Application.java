package com.rcp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// The core entity: one row = one road-cutting permission application.
// Every finder method that touches this table MUST also filter by tenantId - see ApplicationRepository.
@Entity
@Table(name = "application")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    // Internal surrogate key. Never returned to API clients directly (they use applicationNumber).
    @Id
    @GeneratedValue
    private UUID id;

    // Public, human-readable identifier, e.g. DDN-RCP-000123-2026-27.
    @Column(name = "application_number", nullable = false, unique = true)
    private String applicationNumber;

    // The city/tenant this application belongs to. This is the column every query must scope by.
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // roadType is a plain string (BT / CC / WBM / KUTCHA today), validated against the rate
    // configuration at write time rather than modelled as a Java enum - the whole point of the
    // config-driven rate table is that a new road type can be added without a code change.
    @Column(name = "road_type", nullable = false)
    private String roadType;

    @Column(name = "length_in_meters", nullable = false)
    private BigDecimal lengthInMeters;

    @Column(name = "width_in_meters", nullable = false)
    private BigDecimal widthInMeters;

    @Column(name = "area_in_sqm", nullable = false)
    private BigDecimal areaInSqm;

    @Column(name = "duration_in_days", nullable = false)
    private Integer durationInDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicant_type", nullable = false)
    private ApplicantType applicantType;

    @Column(name = "proposed_start_date", nullable = false)
    private LocalDate proposedStartDate;

    // The date the application was created - used as "applicationDate" in the fee-calculation rules.
    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    // --- Persisted fee breakdown. Computed server-side at creation time; never trust a client value. ---
    @Column(name = "restoration_charge", nullable = false)
    private BigDecimal restorationCharge;

    @Column(name = "permission_fee", nullable = false)
    private BigDecimal permissionFee;

    @Column(name = "urgency_surcharge", nullable = false)
    private BigDecimal urgencySurcharge;

    @Column(name = "security_deposit", nullable = false)
    private BigDecimal securityDeposit;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;

    // Identity of the applicant, taken from RequestInfo.userInfo.uuid/userName at creation time.
    @Column(name = "applicant_uuid", nullable = false)
    private String applicantUuid;

    @Column(name = "applicant_mobile_number", nullable = false)
    private String applicantMobileNumber;

    // Optional client-supplied idempotency key. Not required by the Core spec but harmless to carry.
    @Column(name = "request_reference_id")
    private String requestReferenceId;

    // --- Standard audit columns, required by the spec. ---
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_time", nullable = false)
    private Instant createdTime;

    @Column(name = "last_modified_by", nullable = false)
    private String lastModifiedBy;

    @Column(name = "last_modified_time", nullable = false)
    private Instant lastModifiedTime;

    // JPA optimistic locking: every UPDATE checks this value and bumps it. A stale write (based on an
    // old version) fails with OptimisticLockException, which we translate into a 409 Conflict response.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
