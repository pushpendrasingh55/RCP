package com.rcp.controller;

import com.rcp.calculation.FeeBreakdown;
import com.rcp.dto.ResponseInfo;
import com.rcp.dto.request.CalculateRequest;
import com.rcp.dto.response.CalculateResponse;
import com.rcp.dto.response.CalculationResult;
import com.rcp.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CalculationController {

    private final ApplicationService applicationService;

    public CalculationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    // Stateless fee preview. Never creates or modifies an application - see ApplicationService.calculatePreview.
    @PostMapping("/rcp/v1/_calculate")
    public ResponseEntity<CalculateResponse> calculate(@Valid @RequestBody CalculateRequest request) {
        FeeBreakdown breakdown = applicationService.calculatePreview(request.getRequestInfo(), request.getCalculation());

        CalculationResult result = CalculationResult.builder()
                .areaInSqm(breakdown.getAreaInSqm())
                .restorationCharge(breakdown.getRestorationCharge())
                .permissionFee(breakdown.getPermissionFee())
                .urgencySurcharge(breakdown.getUrgencySurcharge())
                .securityDeposit(breakdown.getSecurityDeposit())
                .totalAmount(breakdown.getTotalAmount())
                // reviewRef is a fixed literal required by Addendum Revision 3.1 - see CalculationResult.
                .build();

        ResponseInfo responseInfo = ResponseInfo.successful(request.getRequestInfo().getMsgId());
        return ResponseEntity.ok(new CalculateResponse(responseInfo, result));
    }
}
