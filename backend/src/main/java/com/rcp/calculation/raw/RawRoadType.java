package com.rcp.calculation.raw;

import lombok.Getter;
import lombok.Setter;

// Direct mirror of one road-type object in rates.json. Numeric/boolean fields use boxed wrapper
// types (Integer/Boolean, not int/boolean) so that, inside a TENANT override entry, "field absent
// from the JSON" (null) can be told apart from "field explicitly present" - a primitive int would
// silently default to 0 and corrupt the merge in RateConfigMerger.
@Getter
@Setter
public class RawRoadType {
    private String code;
    private String name;
    private Integer restorationRatePerSqm;
    private Integer permissionRatePerSqmPerDay;
    private Integer minSecurityDeposit;
    private Boolean active;
}
