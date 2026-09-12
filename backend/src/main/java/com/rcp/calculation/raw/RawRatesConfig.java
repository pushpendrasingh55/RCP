package com.rcp.calculation.raw;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

// Root object of rates.json.
@Getter
@Setter
public class RawRatesConfig {
    private RawDefaults defaults;
    private Map<String, RawTenantOverride> tenants;
}
