package com.aios.authz.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The ordered history of one workflow: its actions, keyed by {@code workflowId}
 * rather than by principal — this is what lets a delegated-to agent's
 * authorization request see provenance acquired by a different agent earlier
 * in the same workflow (Scenario F) — plus the {@link Delegation}s recorded
 * against it (ENG-27). See {@link Delegation}'s own note on why delegations
 * are bookkeeping, not something the authorization decision itself depends on.
 *
 * <p>Immutable: {@link #append} and {@link #appendDelegation} each return a
 * new instance rather than mutating this one, so a denied speculative append
 * never corrupts the real trajectory.
 */
public record Trajectory(String workflowId, List<ActionRecord> actions, List<Delegation> delegations) {

    public Trajectory {
        if (workflowId == null || workflowId.isBlank()) {
            throw new IllegalArgumentException("Trajectory workflowId must not be blank");
        }
        actions = actions == null ? List.of() : List.copyOf(actions);
        delegations = delegations == null ? List.of() : List.copyOf(delegations);
    }

    public static Trajectory empty(String workflowId) {
        return new Trajectory(workflowId, List.of(), List.of());
    }

    public Trajectory append(ActionRecord record) {
        Objects.requireNonNull(record, "Cannot append a null ActionRecord");
        List<ActionRecord> next = new ArrayList<>(actions);
        next.add(record);
        return new Trajectory(workflowId, next, delegations);
    }

    public Trajectory appendDelegation(Delegation delegation) {
        Objects.requireNonNull(delegation, "Cannot append a null Delegation");
        List<Delegation> next = new ArrayList<>(delegations);
        next.add(delegation);
        return new Trajectory(workflowId, actions, next);
    }
}
