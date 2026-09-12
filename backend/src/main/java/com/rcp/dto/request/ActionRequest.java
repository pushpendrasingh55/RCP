package com.rcp.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rcp.dto.RequestInfo;
import com.rcp.domain.WorkflowAction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Body of POST /rcp/v1/_action. Root envelope keys are capitalized to match the spec's contract precisely.
@Getter
@Setter
public class ActionRequest {

    @JsonProperty("RequestInfo")
    @NotNull(message = "RequestInfo is required")
    @Valid
    private RequestInfo requestInfo;

    @JsonProperty("ApplicationNumber")
    @NotBlank(message = "applicationNumber is required")
    private String applicationNumber;

    @JsonProperty("Action")
    @NotNull(message = "action is required")
    private WorkflowAction action;

    // Optional remark, e.g. why an application was sent back or rejected. Stored on the history row.
    @JsonProperty("Comment")
    private String comment;
}
