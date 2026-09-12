package com.rcp.mapper;

import com.rcp.domain.Application;
import com.rcp.domain.TransitionHistory;
import com.rcp.domain.WorkflowAction;
import com.rcp.dto.response.ApplicationResponse;
import com.rcp.dto.response.TransitionHistoryEntry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApplicationMapper {

    public ApplicationResponse toResponse(Application application,
                                           List<TransitionHistory> history,
                                           List<WorkflowAction> availableActions) {
        return ApplicationResponse.builder()
                .applicationNumber(application.getApplicationNumber())
                .tenantId(application.getTenantId())
                .roadType(application.getRoadType())
                .lengthInMeters(application.getLengthInMeters())
                .widthInMeters(application.getWidthInMeters())
                .areaInSqm(application.getAreaInSqm())
                .durationInDays(application.getDurationInDays())
                .applicantType(application.getApplicantType())
                .proposedStartDate(application.getProposedStartDate())
                .applicationDate(application.getApplicationDate())
                .restorationCharge(application.getRestorationCharge())
                .permissionFee(application.getPermissionFee())
                .urgencySurcharge(application.getUrgencySurcharge())
                .securityDeposit(application.getSecurityDeposit())
                .totalAmount(application.getTotalAmount())
                .status(application.getStatus())
                .availableActions(availableActions.stream().map(Enum::name).toList())
                .applicantMobileNumber(application.getApplicantMobileNumber())
                .createdTime(application.getCreatedTime())
                .lastModifiedTime(application.getLastModifiedTime())
                .history(history.stream().map(this::toHistoryEntry).toList())
                .build();
    }

    private TransitionHistoryEntry toHistoryEntry(TransitionHistory h) {
        return TransitionHistoryEntry.builder()
                .action(h.getAction())
                .previousState(h.getPreviousState())
                .resultingState(h.getResultingState())
                .actorUuid(h.getActorUuid())
                .actorRole(h.getActorRole())
                .comment(h.getComment())
                .timestamp(h.getCreatedTime())
                .build();
    }
}
