package com.rcp.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rcp.dto.RequestInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Body of POST /rcp/v1/_create. Reuses the same "Calculation" shape used for the preview endpoint,
// so the applicant's confirmed preview and the create payload are guaranteed to be structurally
// identical. Root envelope keys are capitalized to match the spec's contract precisely.
@Getter
@Setter
public class CreateApplicationRequest {

    @JsonProperty("RequestInfo")
    @NotNull(message = "RequestInfo is required")
    @Valid
    private RequestInfo requestInfo;

    @JsonProperty("Calculation")
    @NotNull(message = "Calculation is required")
    @Valid
    private CalculationInput calculation;

    // Optional client-generated idempotency key (e.g. a UUID created when the form is first opened).
    // Not required by the Core spec; included so a flaky-network retry can be recognised as a duplicate.
    @JsonProperty("RequestReferenceId")
    private String requestReferenceId;
}
