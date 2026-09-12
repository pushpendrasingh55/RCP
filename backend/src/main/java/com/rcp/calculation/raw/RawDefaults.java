package com.rcp.calculation.raw;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Mirror of the "defaults" object in rates.json.
@Getter
@Setter
public class RawDefaults {
    private List<RawRoadType> roadTypes;
    private Integer urgencyThresholdDays;
    private Integer urgencySurchargePercent;
    private Integer securityDepositPercent;
}
