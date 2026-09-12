package com.rcp.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rcp.dto.ResponseInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

// Top-level response body for POST /rcp/v1/_search. Root envelope keys are capitalized to match
// the spec's contract precisely.
@Getter
@AllArgsConstructor
public class SearchResponse {
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;
    @JsonProperty("Applications")
    private List<ApplicationResponse> applications;
    @JsonProperty("TotalCount")
    private long totalCount;
}
