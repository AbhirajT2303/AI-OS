package com.aios.authz.state;

/**
 * Thrown when an action is authorized against a {@code workflowId} with no
 * open workflow. Never implicitly opens one — see ENG-16's acceptance
 * criterion: authorizing against an unknown workflow is a 404, not a silent
 * new workflow.
 */
public class UnknownWorkflowException extends RuntimeException {

    public UnknownWorkflowException(String workflowId) {
        super("No open workflow with id: " + workflowId);
    }
}
