package com.rcp.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rcp.domain.ApplicationStatus;
import com.rcp.domain.Role;
import com.rcp.domain.WorkflowAction;
import com.rcp.exception.IllegalTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowServiceTest {

    private WorkflowService workflowService;

    @BeforeEach
    void setUp() throws Exception {
        workflowService = new WorkflowService(new ClassPathResource("workflow-config.json"), new ObjectMapper());
        workflowService.init();
    }

    @Test
    void verifierCanVerifyAnAppliedApplication() {
        ApplicationStatus result = workflowService.applyTransition(
                ApplicationStatus.APPLIED, WorkflowAction.VERIFY, Role.VERIFIER);
        assertThat(result).isEqualTo(ApplicationStatus.PENDING_APPROVAL);
    }

    @Test
    void approverCanApproveAPendingApplication() {
        ApplicationStatus result = workflowService.applyTransition(
                ApplicationStatus.PENDING_APPROVAL, WorkflowAction.APPROVE, Role.APPROVER);
        assertThat(result).isEqualTo(ApplicationStatus.APPROVED);
    }

    @Test
    void sendBackReturnsAnAppliedApplicationToApplied() {
        ApplicationStatus result = workflowService.applyTransition(
                ApplicationStatus.APPLIED, WorkflowAction.SEND_BACK, Role.VERIFIER);
        assertThat(result).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void applicantCanCancelAnAppliedApplication() {
        ApplicationStatus result = workflowService.applyTransition(
                ApplicationStatus.APPLIED, WorkflowAction.CANCEL, Role.APPLICANT);
        assertThat(result).isEqualTo(ApplicationStatus.CANCELLED);
    }

    // ---- Illegal transition: wrong state (required by the spec's testing checklist) ----
    @Test
    void approvingAnAlreadyApprovedApplication_isIllegal() {
        assertThatThrownBy(() -> workflowService.applyTransition(
                ApplicationStatus.APPROVED, WorkflowAction.APPROVE, Role.APPROVER))
                .isInstanceOf(IllegalTransitionException.class)
                .hasMessageContaining("not allowed from state");
    }

    @Test
    void cancelIsOnlyAllowedFromApplied() {
        assertThatThrownBy(() -> workflowService.applyTransition(
                ApplicationStatus.PENDING_APPROVAL, WorkflowAction.CANCEL, Role.APPLICANT))
                .isInstanceOf(IllegalTransitionException.class);
    }

    // ---- Wrong role ----
    @Test
    void applicantAttemptingApprove_isRejected() {
        assertThatThrownBy(() -> workflowService.applyTransition(
                ApplicationStatus.PENDING_APPROVAL, WorkflowAction.APPROVE, Role.APPLICANT))
                .isInstanceOf(IllegalTransitionException.class)
                .hasMessageContaining("not permitted");
    }

    @Test
    void verifierAttemptingApprove_isRejected() {
        assertThatThrownBy(() -> workflowService.applyTransition(
                ApplicationStatus.PENDING_APPROVAL, WorkflowAction.APPROVE, Role.VERIFIER))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void approverAttemptingVerify_isRejected() {
        assertThatThrownBy(() -> workflowService.applyTransition(
                ApplicationStatus.APPLIED, WorkflowAction.VERIFY, Role.APPROVER))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void availableActions_reflectsStateAndRole() {
        assertThat(workflowService.availableActions(ApplicationStatus.APPLIED, Role.VERIFIER))
                .containsExactlyInAnyOrder(WorkflowAction.VERIFY, WorkflowAction.SEND_BACK);
        assertThat(workflowService.availableActions(ApplicationStatus.APPLIED, Role.APPLICANT))
                .containsExactly(WorkflowAction.CANCEL);
        assertThat(workflowService.availableActions(ApplicationStatus.APPROVED, Role.APPROVER))
                .isEmpty();
    }
}
