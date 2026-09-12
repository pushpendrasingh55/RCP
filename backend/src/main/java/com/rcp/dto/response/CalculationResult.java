package com.rcp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// The "Calculation" object returned by POST /rcp/v1/_calculate (and embedded when returning a
// created/updated application). "reviewRef" is a fixed literal required by Addendum Revision 3.1 -
// it is not computed from any input; it exists purely so the response matches the addendum's example byte-for-byte.
@Getter
@Builder
@AllArgsConstructor
public class CalculationResult {
    private BigDecimal areaInSqm;
    private BigDecimal restorationCharge;
    private BigDecimal permissionFee;
    private BigDecimal urgencySurcharge;
    private BigDecimal securityDeposit;
    private BigDecimal totalAmount;
    @Builder.Default
    private String reviewRef = "K7Q2";
}
