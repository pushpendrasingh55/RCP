package com.rcp.dto.response;

import com.rcp.domain.ApplicationStatus;
import com.rcp.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

// One entry in the transition-history timeline returned to the frontend. The frontend must build its
// timeline entirely from a list of these - it must never hardcode the lifecycle stage list itself.
@Getter
@Builder
@AllArgsConstructor
public class TransitionHistoryEntry {
    private String action;
    private ApplicationStatus previousState; // null for the initial CREATE entry
    private ApplicationStatus resultingState;
    private String actorUuid;
    private Role actorRole;
    private String comment;
    private Instant timestamp;
}
