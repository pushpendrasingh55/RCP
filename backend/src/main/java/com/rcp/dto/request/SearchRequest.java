package com.rcp.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rcp.dto.RequestInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Body of POST /rcp/v1/_search. Root envelope keys are capitalized to match the spec's contract precisely.
@Getter
@Setter
public class SearchRequest {

    @JsonProperty("RequestInfo")
    @NotNull(message = "RequestInfo is required")
    @Valid
    private RequestInfo requestInfo;

    // Optional - a request with no criteria object at all means "no filters, just paginate".
    @JsonProperty("SearchCriteria")
    private SearchCriteria searchCriteria;

    @JsonProperty("Offset")
    @Min(value = 0, message = "offset must not be negative")
    private Integer offset = 0;

    // The server clamps this to rcp.search.max-limit regardless of what the client asks for.
    @JsonProperty("Limit")
    @Min(value = 1, message = "limit must be at least 1")
    private Integer limit;
}
