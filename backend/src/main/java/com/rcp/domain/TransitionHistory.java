package com.rcp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// One row per workflow transition (including the initial CREATE). This table is append-only -
// we never update or delete a row here. The frontend timeline is built entirely from these rows,
// so every field the UI needs to render "who did what, when, and what changed" must live here.
@Entity
@Table(name = "transition_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionHistory {

    @Id
    @GeneratedValue
    private UUID id;

    // FK to Application.id. We store it as a raw UUID column (not a JPA @ManyToOne) to keep history
    // writes simple and independent of the parent entity's lifecycle/locking.
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    // Denormalised copy of the application's tenant, so history queries stay tenant-scoped without a join.
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // CREATE, VERIFY, SEND_BACK, APPROVE, REJECT or CANCEL. Stored as a plain string because CREATE
    // is not a member of the WorkflowAction enum (creation happens outside the workflow engine).
    @Column(name = "action", nullable = false)
    private String action;

    // Null only for the very first row (there is no "previous state" before an application exists).
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_state")
    private ApplicationStatus previousState;

    @Enumerated(EnumType.STRING)
    @Column(name = "resulting_state", nullable = false)
    private ApplicationStatus resultingState;

    @Column(name = "actor_uuid", nullable = false)
    private String actorUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false)
    private Role actorRole;

    @Column(name = "comment")
    private String comment;

    @Column(name = "created_time", nullable = false)
    private Instant createdTime;
}
