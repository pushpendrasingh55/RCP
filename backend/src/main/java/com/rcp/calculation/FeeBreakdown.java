package com.rcp.calculation;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

// The pure result of a fee calculation. Lives in the calculation package (not dto) so this service
// has no dependency on the web layer - it could be reused by a batch job or a CLI unchanged.
@Getter
@AllArgsConstructor
public class FeeBreakdown {
    private final BigDecimal areaInSqm;
    private final BigDecimal restorationCharge;
    private final BigDecimal permissionFee;
    private final BigDecimal urgencySurcharge;
    private final BigDecimal securityDeposit;
    private final BigDecimal totalAmount;
}
