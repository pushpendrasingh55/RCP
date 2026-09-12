package com.rcp.calculation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rcp.calculation.raw.RawDefaults;
import com.rcp.calculation.raw.RawRatesConfig;
import com.rcp.calculation.raw.RawRoadType;
import com.rcp.calculation.raw.RawTenantOverride;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// The only implementation of RateConfigurationProvider today. It reads rates.json exactly once, at
// startup, and pre-computes a fully-merged rate table per tenant so that every request-time lookup
// is a plain in-memory map read - no re-parsing, no re-merging, per request.
//
// If rates ever needed to come from a remote configuration service instead, only this class (or a
// new class implementing the same interface) would change; FeeCalculationService would not.
@Component
public class JsonRateConfigurationProvider implements RateConfigurationProvider {

    // Injected as a Spring "Resource" (not a plain file path) so classpath:rates.json resolves
    // correctly both when running from an IDE and from the packaged jar.
    private final Resource ratesResource;
    private final ObjectMapper objectMapper;

    // tenantId -> (roadTypeCode -> resolved rate). Populated once in init().
    private Map<String, Map<String, RoadTypeRate>> resolvedRatesByTenant;
    // The merged-but-unresolved defaults table, used as the fallback for any tenantId with no
    // override entry in rates.json at all (today: any tenant other than dehradun/haridwar).
    private Map<String, RoadTypeRate> defaultRoadTypes;
    private GlobalRateSettings globalRateSettings;

    public JsonRateConfigurationProvider(@Value("classpath:rates.json") Resource ratesResource,
                                          ObjectMapper objectMapper) {
        this.ratesResource = ratesResource;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() throws IOException {
        RawRatesConfig raw;
        try (var in = ratesResource.getInputStream()) {
            raw = objectMapper.readValue(in, RawRatesConfig.class);
        }

        RawDefaults defaults = raw.getDefaults();

        // Step 1: build the default rate for every road type exactly as given in the "defaults" block.
        this.defaultRoadTypes = new HashMap<>();
        for (RawRoadType rawRoadType : defaults.getRoadTypes()) {
            defaultRoadTypes.put(rawRoadType.getCode(), toResolved(rawRoadType));
        }

        this.globalRateSettings = new GlobalRateSettings(
                defaults.getUrgencyThresholdDays(),
                BigDecimal.valueOf(defaults.getUrgencySurchargePercent()),
                BigDecimal.valueOf(defaults.getSecurityDepositPercent())
        );

        // Step 2: for every tenant listed under "tenants", start from a COPY of the defaults map,
        // then overlay only the fields present in that tenant's override entries. A tenant override
        // that omits a field (e.g. Haridwar's BT entry never mentions restorationRatePerSqm) must
        // leave the default value untouched - this is the "merge, don't replace" rule from the spec.
        this.resolvedRatesByTenant = new HashMap<>();
        Map<String, RawTenantOverride> tenants = raw.getTenants() == null ? Map.of() : raw.getTenants();
        for (Map.Entry<String, RawTenantOverride> entry : tenants.entrySet()) {
            String tenantId = entry.getKey();
            Map<String, RoadTypeRate> merged = new HashMap<>(defaultRoadTypes);

            RawTenantOverride override = entry.getValue();
            if (override != null && override.getRoadTypes() != null) {
                for (RawRoadType overrideRoadType : override.getRoadTypes()) {
                    RoadTypeRate base = merged.get(overrideRoadType.getCode());
                    if (base == null) {
                        // A tenant override referencing a road type that doesn't exist in defaults at
                        // all is a configuration error we want to fail loudly on, at startup - not
                        // silently ignore it and risk a confusing "unknown road type" at request time.
                        throw new IllegalStateException("Tenant '" + tenantId
                                + "' overrides unknown road type code '" + overrideRoadType.getCode() + "'");
                    }
                    merged.put(overrideRoadType.getCode(), mergeOverride(base, overrideRoadType));
                }
            }
            resolvedRatesByTenant.put(tenantId, merged);
        }
    }

    @Override
    public Optional<RoadTypeRate> getRoadTypeRate(String tenantId, String roadTypeCode) {
        Map<String, RoadTypeRate> tenantTable = resolvedRatesByTenant.getOrDefault(tenantId, defaultRoadTypes);
        return Optional.ofNullable(tenantTable.get(roadTypeCode));
    }

    @Override
    public GlobalRateSettings getGlobalSettings(String tenantId) {
        // No tenant currently overrides these in rates.json (see GlobalRateSettings' javadoc);
        // the tenantId parameter exists so a future override wouldn't require an interface change.
        return globalRateSettings;
    }

    // Converts a fully-populated raw entry (as every "defaults" entry always is) into a resolved rate.
    private RoadTypeRate toResolved(RawRoadType raw) {
        return new RoadTypeRate(
                raw.getCode(),
                raw.getName(),
                BigDecimal.valueOf(raw.getRestorationRatePerSqm()),
                BigDecimal.valueOf(raw.getPermissionRatePerSqmPerDay()),
                BigDecimal.valueOf(raw.getMinSecurityDeposit()),
                Boolean.TRUE.equals(raw.getActive())
        );
    }

    // Applies only the non-null fields of a tenant override onto a base (default) resolved rate.
    private RoadTypeRate mergeOverride(RoadTypeRate base, RawRoadType override) {
        return new RoadTypeRate(
                base.getCode(),
                override.getName() != null ? override.getName() : base.getName(),
                override.getRestorationRatePerSqm() != null
                        ? BigDecimal.valueOf(override.getRestorationRatePerSqm()) : base.getRestorationRatePerSqm(),
                override.getPermissionRatePerSqmPerDay() != null
                        ? BigDecimal.valueOf(override.getPermissionRatePerSqmPerDay()) : base.getPermissionRatePerSqmPerDay(),
                override.getMinSecurityDeposit() != null
                        ? BigDecimal.valueOf(override.getMinSecurityDeposit()) : base.getMinSecurityDeposit(),
                override.getActive() != null ? override.getActive() : base.isActive()
        );
    }
}
