package com.rcp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// The common response envelope header, echoed on both success and failure responses.
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseInfo {

    private String msgId;
    private String status; // "successful" or "failed"

    public static ResponseInfo successful(String msgId) {
        return new ResponseInfo(msgId, "successful");
    }

    public static ResponseInfo failed() {
        return new ResponseInfo(null, "failed");
    }
}
