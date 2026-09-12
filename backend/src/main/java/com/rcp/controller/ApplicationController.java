package com.rcp.controller;

import com.rcp.dto.ResponseInfo;
import com.rcp.dto.request.ActionRequest;
import com.rcp.dto.request.CreateApplicationRequest;
import com.rcp.dto.request.SearchRequest;
import com.rcp.dto.response.ActionResponse;
import com.rcp.dto.response.ApplicationResponse;
import com.rcp.dto.response.SearchResponse;
import com.rcp.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/rcp/v1/_create")
    public ResponseEntity<ActionResponse> create(@Valid @RequestBody CreateApplicationRequest request) {
        ApplicationResponse application = applicationService.create(request);
        ResponseInfo responseInfo = ResponseInfo.successful(request.getRequestInfo().getMsgId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ActionResponse(responseInfo, application));
    }

    @PostMapping("/rcp/v1/_action")
    public ResponseEntity<ActionResponse> action(@Valid @RequestBody ActionRequest request) {
        ApplicationResponse application = applicationService.performAction(request);
        ResponseInfo responseInfo = ResponseInfo.successful(request.getRequestInfo().getMsgId());
        return ResponseEntity.ok(new ActionResponse(responseInfo, application));
    }

    @PostMapping("/rcp/v1/_search")
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        long[] totalCountOut = new long[1];
        List<ApplicationResponse> applications = applicationService.search(request, totalCountOut);
        ResponseInfo responseInfo = ResponseInfo.successful(request.getRequestInfo().getMsgId());
        return ResponseEntity.ok(new SearchResponse(responseInfo, applications, totalCountOut[0]));
    }
}
