package com.rcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

// { "ResponseInfo": { "status": "failed" }, "Errors": [ { "code": ..., "message": ... } ] }
@Getter
public class ErrorResponse {
    @JsonProperty("ResponseInfo")
    private final ResponseInfo responseInfo = ResponseInfo.failed();
    @JsonProperty("Errors")
    private final List<ErrorDetail> errors;

    public ErrorResponse(List<ErrorDetail> errors) {
        this.errors = errors;
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(List.of(new ErrorDetail(code, message)));
    }
}
