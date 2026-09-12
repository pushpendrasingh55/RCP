package com.rcp.workflow;

import com.rcp.domain.ApplicationStatus;
import com.rcp.domain.Role;
import com.rcp.domain.WorkflowAction;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// One row of the workflow table: "from this state, this action, by one of these roles, goes to that state".
// Fields are mutable (plain getters/setters) purely so Jackson can populate them directly from workflow-config.json.
@Getter
@Setter
public class WorkflowTransition {
    private ApplicationStatus fromState;
    private WorkflowAction action;
    private ApplicationStatus toState;
    private List<Role> allowedRoles;

    public boolean permits(Role role) {
        return allowedRoles != null && allowedRoles.contains(role);
    }
}
