package com.rcp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// A single role entry inside userInfo.roles, e.g. { "code": "APPLICANT" }.
@Getter
@Setter
public class UserRole {

    @NotBlank(message = "role code is required")
    private String code;
}
