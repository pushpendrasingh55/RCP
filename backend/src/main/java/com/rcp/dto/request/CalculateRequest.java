package com.rcp.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rcp.dto.RequestInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Body of POST /rcp/v1/_calculate. Root envelope keys are capitalized (RequestInfo, Calculation) to
// match the spec's contract precisely; everything nested inside them stays lowercase camelCase,
// exactly as the spec's own worked example shows.
@Getter
@Setter
public class CalculateRequest {

    @JsonProperty("RequestInfo")
    @NotNull(message = "RequestInfo is required")
    @Valid
    private RequestInfo requestInfo;

    @JsonProperty("Calculation")
    @NotNull(message = "Calculation is required")
    @Valid
    private CalculationInput calculation;
}
