package com.rcp.calculation;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

// The tenant-wide (not road-type-specific) percentages/thresholds from the "defaults" block.
// The sample rates.json never overrides these per-tenant, so today every tenant gets the same values -
// but the abstraction (RateConfigurationProvider.getGlobalSettings(tenantId)) already takes a tenantId,
// so a future per-tenant override would not require touching FeeCalculationService.
@Getter
@AllArgsConstructor
public class GlobalRateSettings {
    private final int urgencyThresholdDays;
    private final BigDecimal urgencySurchargePercent;
    private final BigDecimal securityDepositPercent;
}
