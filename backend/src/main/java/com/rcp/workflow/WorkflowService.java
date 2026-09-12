package com.rcp.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rcp.domain.ApplicationStatus;
import com.rcp.domain.Role;
import com.rcp.domain.WorkflowAction;
import com.rcp.exception.IllegalTransitionException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

// The workflow engine. It knows nothing about roads, fees, or applications - only about
// (state, action, role) -> state. Every rule it enforces comes from workflow-config.json;
// there is deliberately no switch/if-else on ApplicationStatus anywhere in this class.
//
// To change the lifecycle (e.g. add a new state, let VERIFIER also CANCEL, etc.) you edit
// workflow-config.json and redeploy - no Java code here needs to change.
@Service
public class WorkflowService {

    private final Resource workflowConfigResource;
    private final ObjectMapper objectMapper;
    private List<WorkflowTransition> transitions;

    public WorkflowService(@Value("classpath:workflow-config.json") Resource workflowConfigResource,
                            ObjectMapper objectMapper) {
        this.workflowConfigResource = workflowConfigResource;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() throws IOException {
        try (var in = workflowConfigResource.getInputStream()) {
            this.transitions = List.of(objectMapper.readValue(in, WorkflowTransition[].class));
        }
    }

    // Finds the single configured transition (if any) matching the current state + requested action,
    // regardless of role. Used to distinguish "no such transition exists at all" from
    // "the transition exists but this actor's role is not allowed to perform it" - the two produce
    // different, more useful error messages.
    private Optional<WorkflowTransition> findTransition(ApplicationStatus fromState, WorkflowAction action) {
        return transitions.stream()
                .filter(t -> t.getFromState() == fromState && t.getAction() == action)
                .findFirst();
    }

    // Returns the target state if (fromState, action, role) is a legal transition, otherwise throws
    // an IllegalTransitionException with a message explaining exactly what was wrong.
    public ApplicationStatus applyTransition(ApplicationStatus fromState, WorkflowAction action, Role actorRole) {
        WorkflowTransition transition = findTransition(fromState, action)
                .orElseThrow(() -> new IllegalTransitionException(
                        "Action " + action + " is not allowed from state " + fromState));

        if (!transition.permits(actorRole)) {
            throw new IllegalTransitionException(
                    "Role " + actorRole + " is not permitted to perform action " + action);
        }

        return transition.getToState();
    }

    // Used by the API/UI to tell the caller which actions are available on an application right now,
    // given their role - this is what the frontend's action buttons are built from, instead of a
    // hardcoded switch statement in a React component.
    public List<WorkflowAction> availableActions(ApplicationStatus currentState, Role actorRole) {
        return transitions.stream()
                .filter(t -> t.getFromState() == currentState && t.permits(actorRole))
                .map(WorkflowTransition::getAction)
                .toList();
    }
}
