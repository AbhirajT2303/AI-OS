package com.aios.authz.state;

import com.aios.authz.domain.Delegation;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.Trajectory;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Opens new workflows, looks up their current state, and records delegations
 * against them. Opening, not the API controller, is where a workflow id is
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

    public Optional<AuthorizationState> find(String workflowId) {
        return stateStore.find(workflowId);
    }

    /**
     * Records a transfer of specific data from one principal to another.
     * Rejects a data id the workflow does not currently hold — see
     * {@link Delegation}'s own note that this is bookkeeping, not something
     * the authorization decision depends on: the transfer succeeding or not
     * has no bearing on whether {@code ProvenanceBoundaryPolicy} would deny a
     * later send, since state is already workflow-scoped.
     */
    public void delegate(String workflowId, String fromPrincipalId, String toPrincipalId, Set<String> transferredDataIds) {
        AuthorizationState state = stateStore.find(workflowId)
            .orElseThrow(() -> new UnknownWorkflowException(workflowId));

        for (String dataId : transferredDataIds) {
            if (state.provenanceGraph().find(dataId).isEmpty()) {
                throw new IllegalArgumentException(
                    "Cannot delegate '" + dataId + "': workflow '" + workflowId + "' does not hold it");
            }
        }

        Delegation delegation = new Delegation(fromPrincipalId, toPrincipalId, transferredDataIds, Instant.now());
        Trajectory nextTrajectory = state.trajectory().appendDelegation(delegation);
        AuthorizationState nextState = new AuthorizationState(
            state.workflowId(), state.initiator(), state.intent(), nextTrajectory, state.provenanceGraph());
        stateStore.save(nextState);
    }
}
