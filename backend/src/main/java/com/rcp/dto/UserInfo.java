package com.rcp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Identity of the caller. In a real deployment this would come from a verified session/token;
// here (auth is explicitly out of scope) we simply trust the body, as the spec requires.
@Getter
@Setter
public class UserInfo {

    @NotBlank(message = "userInfo.uuid is required")
    private String uuid;

    // Used as the applicant's mobile number when they create an application, and as a search filter.
    @NotBlank(message = "userInfo.userName is required")
    private String userName;

    // The tenant (city) this user belongs to. This is the SERVER-SIDE-AUTHORITATIVE tenant identity -
    // see TenantContext for how it is reconciled against any tenantId sent inside a request body.
    @NotBlank(message = "userInfo.tenantId is required")
    private String tenantId;

    @NotEmpty(message = "userInfo.roles must contain at least one role")
    @Valid
    private List<UserRole> roles;
}
