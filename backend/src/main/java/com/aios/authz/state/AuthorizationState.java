package com.aios.authz.state;

import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.Trajectory;

import java.util.Objects;
import java.util.Set;

/**
 * The security-relevant state of one workflow, used to evaluate the next
 * proposed action.
 *
 * <p>Sprint 2 scope note: this tracks {@code heldAssets} — the {@link DataAsset}s
 * directly read into the workflow — rather than a full provenance graph.
 * {@code ProvenanceGraph} and derived-data lineage are ENG-22/23 (Sprint 3).
 * This is enough for Sprint 2's policies (which key off "has any
 * confidential-or-above data ever been read", not "is this specific output
 * derived from it"), and it composes cleanly with the richer model later:
 * {@code heldAssets} becomes the set of provenance-graph roots.
 *
 * <p>{@code capabilities}, {@code riskBudget} and {@code allowedDestinations}
 * from the original sketch (docs/DOMAIN_MODEL.md §6) are omitted — no Sprint 2
 * policy uses them, and {@code RiskBudgetPolicy} is ENG-42 (Sprint 4).
 */
public record AuthorizationState(
        String workflowId,
        Principal initiator,
        Intent intent,
        Trajectory trajectory,
        Set<DataAsset> heldAssets) {

    public AuthorizationState {
        if (workflowId == null || workflowId.isBlank()) {
            throw new IllegalArgumentException("AuthorizationState workflowId must not be blank");
        }
        Objects.requireNonNull(initiator, "AuthorizationState initiator must not be null");
        Objects.requireNonNull(intent, "AuthorizationState intent must not be null");
        Objects.requireNonNull(trajectory, "AuthorizationState trajectory must not be null");
        heldAssets = heldAssets == null ? Set.of() : Set.copyOf(heldAssets);
    }

    /** The initial state of a freshly opened workflow: no actions, no held data. */
    public static AuthorizationState open(String workflowId, Principal initiator, Intent intent) {
        return new AuthorizationState(workflowId, initiator, intent, Trajectory.empty(workflowId), Set.of());
    }
}
