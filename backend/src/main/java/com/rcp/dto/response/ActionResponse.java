package com.rcp.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rcp.dto.ResponseInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Top-level response body for POST /rcp/v1/_create and POST /rcp/v1/_action - both return
// the (updated) application in full, including its refreshed history and available-actions list.
// Root envelope keys are capitalized to match the spec's contract precisely.
@Getter
@AllArgsConstructor
public class ActionResponse {
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;
    @JsonProperty("Application")
    private ApplicationResponse application;
}
