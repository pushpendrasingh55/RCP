package com.rcp.service;

import com.rcp.calculation.FeeBreakdown;
import com.rcp.calculation.FeeCalculationService;
import com.rcp.domain.*;
import com.rcp.dto.RequestInfo;
import com.rcp.dto.UserRole;
import com.rcp.dto.request.*;
import com.rcp.dto.response.ApplicationResponse;
import com.rcp.exception.ApplicationNotFoundException;
import com.rcp.exception.IllegalTransitionException;
import com.rcp.mapper.ApplicationMapper;
import com.rcp.repository.ApplicationRepository;
import com.rcp.repository.ApplicationSpecifications;
import com.rcp.repository.TransitionHistoryRepository;
import com.rcp.config.RcpProperties;
import com.rcp.validation.InputValidator;
import com.rcp.validation.TenantValidator;
import com.rcp.workflow.WorkflowService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ApplicationService {

    private final FeeCalculationService feeCalculationService;
    private final WorkflowService workflowService;
    private final ApplicationRepository applicationRepository;
    private final TransitionHistoryRepository transitionHistoryRepository;
    private final ApplicationNumberGenerator applicationNumberGenerator;
    private final ApplicationMapper applicationMapper;
    private final TenantValidator tenantValidator;
    private final InputValidator inputValidator;
    private final RcpProperties rcpProperties;

    public ApplicationService(FeeCalculationService feeCalculationService,
                               WorkflowService workflowService,
                               ApplicationRepository applicationRepository,
                               TransitionHistoryRepository transitionHistoryRepository,
                               ApplicationNumberGenerator applicationNumberGenerator,
                               ApplicationMapper applicationMapper,
                               TenantValidator tenantValidator,
                               InputValidator inputValidator,
                               RcpProperties rcpProperties) {
        this.feeCalculationService = feeCalculationService;
        this.workflowService = workflowService;
        this.applicationRepository = applicationRepository;
        this.transitionHistoryRepository = transitionHistoryRepository;
        this.applicationNumberGenerator = applicationNumberGenerator;
        this.applicationMapper = applicationMapper;
        this.tenantValidator = tenantValidator;
        this.inputValidator = inputValidator;
        this.rcpProperties = rcpProperties;
    }

    // ------------------------------------------------------------------
    // _calculate - stateless preview, never touches the database.
    // ------------------------------------------------------------------
    public FeeBreakdown calculatePreview(RequestInfo requestInfo, CalculationInput input) {
        tenantValidator.assertConsistent(requestInfo, input.getTenantId());
        inputValidator.assertProposedStartDateNotInPast(input, LocalDate.now());

        return feeCalculationService.calculate(
                input.getTenantId(),
                input.getRoadType(),
                input.getLengthInMeters(),
                input.getWidthInMeters(),
                input.getDurationInDays(),
                input.getApplicantType(),
                input.getProposedStartDate(),
                LocalDate.now()
        );
    }

    // ------------------------------------------------------------------
    // _create
    // ------------------------------------------------------------------
    @Transactional
    public ApplicationResponse create(CreateApplicationRequest request) {
        RequestInfo requestInfo = request.getRequestInfo();
        CalculationInput input = request.getCalculation();
        String tenantId = requestInfo.getUserInfo().getTenantId();

        tenantValidator.assertConsistent(requestInfo, input.getTenantId());
        inputValidator.assertProposedStartDateNotInPast(input, LocalDate.now());

        // Idempotency: if the client supplied a requestReferenceId we have already seen for this
        // tenant, return the previously-created application instead of creating a duplicate. This
        // protects against a form being submitted twice by a flaky mobile connection.
        if (request.getRequestReferenceId() != null && !request.getRequestReferenceId().isBlank()) {
            Optional<Application> existing = applicationRepository
                    .findByTenantIdAndRequestReferenceId(tenantId, request.getRequestReferenceId());
            if (existing.isPresent()) {
                return toResponseWithHistory(existing.get(), requestInfo);
            }
        }

        LocalDate applicationDate = LocalDate.now();

        // The fee is ALWAYS computed here, from the validated inputs - never from any monetary field
        // the client might have sent alongside the calculation inputs (CalculationInput has no such field at all).
        FeeBreakdown fee = feeCalculationService.calculate(
                tenantId,
                input.getRoadType(),
                input.getLengthInMeters(),
                input.getWidthInMeters(),
                input.getDurationInDays(),
                input.getApplicantType(),
                input.getProposedStartDate(),
                applicationDate
        );

        String applicationNumber = applicationNumberGenerator.generate(tenantId, applicationDate);
        String actorUuid = requestInfo.getUserInfo().getUuid();
        Instant now = Instant.now();

        Application application = Application.builder()
                .applicationNumber(applicationNumber)
                .tenantId(tenantId)
                .roadType(input.getRoadType())
                .lengthInMeters(input.getLengthInMeters())
                .widthInMeters(input.getWidthInMeters())
                .areaInSqm(fee.getAreaInSqm())
                .durationInDays(input.getDurationInDays())
                .applicantType(input.getApplicantType())
                .proposedStartDate(input.getProposedStartDate())
                .applicationDate(applicationDate)
                .restorationCharge(fee.getRestorationCharge())
                .permissionFee(fee.getPermissionFee())
                .urgencySurcharge(fee.getUrgencySurcharge())
                .securityDeposit(fee.getSecurityDeposit())
                .totalAmount(fee.getTotalAmount())
                .status(ApplicationStatus.APPLIED)
                .applicantUuid(actorUuid)
                .applicantMobileNumber(requestInfo.getUserInfo().getUserName())
                .requestReferenceId(request.getRequestReferenceId())
                .createdBy(actorUuid)
                .createdTime(now)
                .lastModifiedBy(actorUuid)
                .lastModifiedTime(now)
                .build();

        application = applicationRepository.save(application);

        TransitionHistory initialHistory = TransitionHistory.builder()
                .applicationId(application.getId())
                .tenantId(tenantId)
                .action("CREATE")
                .previousState(null)
                .resultingState(ApplicationStatus.APPLIED)
                .actorUuid(actorUuid)
                .actorRole(Role.APPLICANT)
                .comment(null)
                .createdTime(now)
                .build();
        transitionHistoryRepository.save(initialHistory);

        return toResponseWithHistory(application, requestInfo);
    }

    // ------------------------------------------------------------------
    // _action
    // ------------------------------------------------------------------
    @Transactional
    public ApplicationResponse performAction(ActionRequest request) {
        RequestInfo requestInfo = request.getRequestInfo();
        String tenantId = requestInfo.getUserInfo().getTenantId();

        // Load-for-update: takes a row lock so a second, concurrent action on the same application
        // blocks until this transaction commits (or fails), rather than racing it. @Version on the
        // entity is the second, independent guard - if somehow both transactions still interleave,
        // the loser's UPDATE fails its version check and we translate that into a 409.
        Application application = applicationRepository.findForUpdate(tenantId, request.getApplicationNumber())
                .orElseThrow(() -> new ApplicationNotFoundException(request.getApplicationNumber()));

        List<Role> actorRoles = resolveRoles(requestInfo.getUserInfo().getRoles());
        if (actorRoles.isEmpty()) {
            throw new IllegalTransitionException("Caller has no recognised role");
        }

        // CANCEL is further restricted to the application's own applicant, not just "any APPLICANT".
        if (request.getAction() == WorkflowAction.CANCEL
                && actorRoles.contains(Role.APPLICANT)
                && !application.getApplicantUuid().equals(requestInfo.getUserInfo().getUuid())) {
            throw new IllegalTransitionException("Only the original applicant may cancel this application");
        }

        // A caller may (in theory) carry more than one role; the transition succeeds if ANY of their
        // roles is permitted. We remember the specific role that succeeded, purely to record it accurately
        // in the history row - "who acted, wearing which hat".
        ApplicationStatus fromState = application.getStatus();
        ApplicationStatus toState = null;
        Role actingRole = null;
        IllegalTransitionException lastFailure = null;
        for (Role role : actorRoles) {
            try {
                toState = workflowService.applyTransition(fromState, request.getAction(), role);
                actingRole = role;
                break;
            } catch (IllegalTransitionException e) {
                lastFailure = e;
            }
        }
        if (toState == null) {
            throw lastFailure;
        }

        Instant now = Instant.now();
        application.setStatus(toState);
        application.setLastModifiedBy(requestInfo.getUserInfo().getUuid());
        application.setLastModifiedTime(now);
        applicationRepository.save(application);

        TransitionHistory historyEntry = TransitionHistory.builder()
                .applicationId(application.getId())
                .tenantId(tenantId)
                .action(request.getAction().name())
                .previousState(fromState)
                .resultingState(toState)
                .actorUuid(requestInfo.getUserInfo().getUuid())
                .actorRole(actingRole)
                .comment(request.getComment())
                .createdTime(now)
                .build();
        transitionHistoryRepository.save(historyEntry);

        return toResponseWithHistory(application, requestInfo);
    }

    // ------------------------------------------------------------------
    // _search
    // ------------------------------------------------------------------
    public List<ApplicationResponse> search(SearchRequest request, long[] totalCountOut) {
        RequestInfo requestInfo = request.getRequestInfo();
        String tenantId = requestInfo.getUserInfo().getTenantId();
        List<Role> actorRoles = resolveRoles(requestInfo.getUserInfo().getRoles());

        SearchCriteria criteria = request.getSearchCriteria() != null ? request.getSearchCriteria() : new SearchCriteria();

        // Tenant scoping is unconditional and is the FIRST predicate added - every other filter is
        // additive on top of it, never a substitute for it.
        Specification<Application> spec = ApplicationSpecifications.tenantId(tenantId);
        spec = spec.and(ApplicationSpecifications.applicationNumber(criteria.getApplicationNumber()));
        spec = spec.and(ApplicationSpecifications.status(criteria.getStatus()));
        spec = spec.and(ApplicationSpecifications.mobileNumber(criteria.getMobileNumber()));

        // An applicant-only caller (no officer role) may only ever see their own applications,
        // regardless of what mobileNumber/etc. they pass in - we force this filter on top, we don't
        // just trust the client to only ever ask for its own records.
        boolean isOfficer = actorRoles.contains(Role.VERIFIER) || actorRoles.contains(Role.APPROVER);
        if (!isOfficer) {
            spec = spec.and(ApplicationSpecifications.applicantUuid(requestInfo.getUserInfo().getUuid()));
        }

        int limit = resolveLimit(request.getLimit());
        int offset = request.getOffset() != null ? Math.max(0, request.getOffset()) : 0;

        // Spring Data's Pageable is page-based, not offset-based; we translate offset/limit into a
        // page index (which only works cleanly when offset is a multiple of limit, true for normal
        // "next page" navigation - documented as an assumption in the README).
        int page = offset / limit;
        var pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdTime"));

        var result = applicationRepository.findAll(spec, pageable);
        totalCountOut[0] = result.getTotalElements();

        return result.getContent().stream()
                .map(app -> toResponseWithHistory(app, requestInfo))
                .toList();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private int resolveLimit(Integer requested) {
        int defaultLimit = rcpProperties.getSearch().getDefaultLimit();
        int maxLimit = rcpProperties.getSearch().getMaxLimit();
        if (requested == null) {
            return defaultLimit;
        }
        return Math.min(Math.max(requested, 1), maxLimit);
    }

    private List<Role> resolveRoles(List<UserRole> userRoles) {
        List<Role> roles = new ArrayList<>();
        if (userRoles == null) {
            return roles;
        }
        for (UserRole userRole : userRoles) {
            try {
                roles.add(Role.valueOf(userRole.getCode()));
            } catch (IllegalArgumentException ignored) {
                // Unknown role codes are ignored rather than rejected outright - a caller might
                // legitimately carry other, unrelated role codes from a wider platform.
            }
        }
        return roles;
    }

    private ApplicationResponse toResponseWithHistory(Application application, RequestInfo requestInfo) {
        List<TransitionHistory> history = transitionHistoryRepository
                .findByTenantIdAndApplicationIdOrderByCreatedTimeAsc(application.getTenantId(), application.getId());

        List<Role> actorRoles = resolveRoles(requestInfo.getUserInfo().getRoles());
        List<WorkflowAction> available = actorRoles.stream()
                .flatMap(role -> workflowService.availableActions(application.getStatus(), role).stream())
                .distinct()
                .toList();

        return applicationMapper.toResponse(application, history, available);
    }
}
