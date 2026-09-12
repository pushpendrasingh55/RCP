package com.rcp.dto.request;

import lombok.Getter;
import lombok.Setter;

// Optional filters for POST /rcp/v1/_search. Any combination (including none) is valid;
// tenant scoping is applied separately and unconditionally by the service layer.
@Getter
@Setter
public class SearchCriteria {
    private String applicationNumber;
    private String status;
    private String mobileNumber;
}
