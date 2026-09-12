package com.rcp.service;

import com.rcp.calculation.FeeCalculationService;
import com.rcp.config.RcpProperties;
import com.rcp.domain.WorkflowAction;
import com.rcp.dto.RequestInfo;
import com.rcp.dto.UserInfo;
import com.rcp.dto.UserRole;
import com.rcp.dto.request.ActionRequest;
import com.rcp.exception.ApplicationNotFoundException;
import com.rcp.mapper.ApplicationMapper;
import com.rcp.repository.ApplicationRepository;
import com.rcp.repository.TransitionHistoryRepository;
import com.rcp.validation.InputValidator;
import com.rcp.validation.TenantValidator;
import com.rcp.workflow.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Proves the tenant-isolation guarantee at the SERVICE layer: an officer authenticated (via
// RequestInfo.userInfo) against one tenant can never load or act on an application belonging to
// another tenant, even if they know its exact application number. The database-level half of this
// guarantee (the repository query itself always filters by tenant_id - see ApplicationRepository
// and the integration notes in README.md "Tenant isolation") is exercised against a real Postgres
// instance, not mocked here.
@ExtendWith(MockitoExtension.class)
class ApplicationServiceTenantIsolationTest {

    @Mock private FeeCalculationService feeCalculationService;
    @Mock private WorkflowService workflowService;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private TransitionHistoryRepository transitionHistoryRepository;
    @Mock private ApplicationNumberGenerator applicationNumberGenerator;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private TenantValidator tenantValidator;
    @Mock private InputValidator inputValidator;
    @Mock private RcpProperties rcpProperties;

    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationService(
                feeCalculationService, workflowService, applicationRepository, transitionHistoryRepository,
                applicationNumberGenerator, applicationMapper, tenantValidator, inputValidator, rcpProperties);
    }

    @Test
    void haridwarOfficerCannotActOnADehradunApplication_evenKnowingItsNumber() {
        // A Haridwar VERIFIER sends an action for an application number that happens to belong to Dehradun.
        ActionRequest request = new ActionRequest();
        RequestInfo requestInfo = new RequestInfo();
        UserInfo userInfo = new UserInfo();
        userInfo.setUuid("officer-1");
        userInfo.setUserName("9990000002");
        userInfo.setTenantId("haridwar");
        UserRole role = new UserRole();
        role.setCode("VERIFIER");
        userInfo.setRoles(List.of(role));
        requestInfo.setUserInfo(userInfo);
        request.setRequestInfo(requestInfo);
        request.setApplicationNumber("DDN-RCP-000123-2026-27");
        request.setAction(WorkflowAction.VERIFY);

        // The repository is scoped by (tenantId, applicationNumber): looking up a Dehradun application
        // number under the Haridwar tenant must find nothing, exactly as it would in the real
        // "WHERE tenant_id = ? AND application_number = ?" query.
        when(applicationRepository.findForUpdate("haridwar", "DDN-RCP-000123-2026-27"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.performAction(request))
                .isInstanceOf(ApplicationNotFoundException.class);

        // Critically: the service must have looked the application up scoped to the CALLER'S tenant
        // (haridwar, from RequestInfo.userInfo), never the raw application number alone and never any
        // tenant implied by the application number's own DDN prefix.
        verify(applicationRepository).findForUpdate(eq("haridwar"), eq("DDN-RCP-000123-2026-27"));
    }
}
