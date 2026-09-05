package com.aios.authz.state;

import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.Trajectory;
import com.aios.authz.provenance.ProvenanceGraph;

import java.util.Objects;
import java.util.Set;

/**
 * The security-relevant state of one workflow, used to evaluate the next
 * proposed action.
 *
 * <p>Tracks a full {@link ProvenanceGraph} (ENG-22/ENG-23) rather than the flat
 * {@code Set<DataAsset>} Sprint 2 used — {@link #heldAssets()} is kept as a
 * convenience view (every {@link DataAsset} root the graph currently knows
 * about) so Sprint 2's coarse policies keep working unchanged against the
 * richer model underneath.
 *
 * <p>{@code capabilities}, {@code riskBudget} and {@code allowedDestinations}
 * from the original sketch (docs/DOMAIN_MODEL.md §6) remain omitted — no
 * policy uses them yet; {@code RiskBudgetPolicy} is ENG-42 (Sprint 4).
 */
public record AuthorizationState(
        String workflowId,
        Principal initiator,
        Intent intent,
        Trajectory trajectory,
        ProvenanceGraph provenanceGraph) {

    public AuthorizationState {
        if (workflowId == null || workflowId.isBlank()) {
            throw new IllegalArgumentException("AuthorizationState workflowId must not be blank");
        }
        Objects.requireNonNull(initiator, "AuthorizationState initiator must not be null");
        Objects.requireNonNull(intent, "AuthorizationState intent must not be null");
        Objects.requireNonNull(trajectory, "AuthorizationState trajectory must not be null");
        provenanceGraph = provenanceGraph == null ? new ProvenanceGraph() : provenanceGraph;
    }

    /** The initial state of a freshly opened workflow: no actions, no known data. */
    public static AuthorizationState open(String workflowId, Principal initiator, Intent intent) {
        return new AuthorizationState(
            workflowId, initiator, intent, Trajectory.empty(workflowId), new ProvenanceGraph());
    }

    /** Every DataAsset root the workflow's provenance graph currently knows about. */
    public Set<DataAsset> heldAssets() {
        return provenanceGraph.allRoots();
    }
}
