package com.rcp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// One entry in the "Errors" array of an error response, per the spec's required error envelope.
@Getter
@AllArgsConstructor
public class ErrorDetail {
    private String code;
    private String message;
}
