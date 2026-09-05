package com.aios.authz.state;

import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;

import java.util.Objects;
import java.util.UUID;

/**
 * Opens new workflows. This, not the API controller, is where a workflow id is
 * minted and its initial state persisted — keeping that decision out of the
 * HTTP layer so it stays testable without a servlet context.
 */
public final class WorkflowManager {

    private final AuthorizationStateStore stateStore;

    public WorkflowManager(AuthorizationStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore must not be null");
    }

    public String open(Principal initiator, Intent intent) {
        String workflowId = "wf-" + UUID.randomUUID();
        stateStore.save(AuthorizationState.open(workflowId, initiator, intent));
        return workflowId;
    }
}
