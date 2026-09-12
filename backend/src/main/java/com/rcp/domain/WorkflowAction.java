package com.rcp.domain;

// The five actions that can be requested via POST /rcp/v1/_action.
// CREATE is deliberately not here: creation happens through /_create, not /_action.
public enum WorkflowAction {
    VERIFY,
    SEND_BACK,
    APPROVE,
    REJECT,
    CANCEL
}
