package com.rcp.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rcp.dto.ResponseInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Top-level response body for POST /rcp/v1/_calculate. Root envelope keys are capitalized to match
// the spec's contract precisely.
@Getter
@AllArgsConstructor
public class CalculateResponse {
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;
    @JsonProperty("Calculation")
    private CalculationResult calculation;
}
