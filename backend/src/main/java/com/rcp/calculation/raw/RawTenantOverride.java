package com.rcp.calculation.raw;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Mirror of one entry under "tenants" in rates.json, e.g. the "haridwar" object.
// Today it only ever contains a partial roadTypes list, but is its own type so a future revision
// could add tenant-level overrides of urgencyThresholdDays etc. without reshaping RawDefaults.
@Getter
@Setter
public class RawTenantOverride {
    private List<RawRoadType> roadTypes;
}
