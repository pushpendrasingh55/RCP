package com.rcp.calculation;

import java.util.Optional;

// Abstraction the calculation code depends on, instead of depending on a JSON file directly.
// A future implementation could fetch these values from a remote configuration service on every
// call (or on a cache-refresh timer) without FeeCalculationService changing at all.
public interface RateConfigurationProvider {

    // Returns the resolved (defaults merged with tenant overrides) rate for the given road-type code
    // under the given tenant, or empty if the code is not recognised at all (not even in defaults).
    // Callers must separately check RoadTypeRate.isActive() - an inactive-but-known code is NOT "empty" here.
    Optional<RoadTypeRate> getRoadTypeRate(String tenantId, String roadTypeCode);

    GlobalRateSettings getGlobalSettings(String tenantId);
}
