package com.rcp.calculation;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

// A fully-resolved (defaults + tenant override already merged) rate for a single road type.
// This is what FeeCalculationService actually consumes - it never sees the raw JSON structure.
@Getter
@AllArgsConstructor
public class RoadTypeRate {
    private final String code;
    private final String name;
    private final BigDecimal restorationRatePerSqm;
    private final BigDecimal permissionRatePerSqmPerDay;
    private final BigDecimal minSecurityDeposit;
    private final boolean active;
}
