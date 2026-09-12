package com.rcp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// The common envelope wrapping every inbound request. Modelled exactly on the spec's example payload.
@Getter
@Setter
public class RequestInfo {

    // Caller-supplied identifier for the calling application (e.g. "portal"). Purely informational.
    private String apiId;

    // Echoed back in ResponseInfo.msgId so the caller can correlate request/response.
    private String msgId;

    // The one and only source of caller identity, since there is no authentication/login system.
    @NotNull(message = "RequestInfo.userInfo is required")
    @Valid
    private UserInfo userInfo;
}
