package com.aios.authz.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The ordered history of one workflow's actions, keyed by {@code workflowId}
 * rather than by principal — this is what will let a delegated-to agent's
 * authorization request see provenance acquired by a different agent earlier
 * in the same workflow (Scenario F, ENG-27/ENG-28, Sprint 4).
 *
 * <p>Immutable: {@link #append} returns a new instance rather than mutating
 * this one, so a denied speculative append never corrupts the real trajectory.
 */
public record Trajectory(String workflowId, List<ActionRecord> actions) {

    public Trajectory {
        if (workflowId == null || workflowId.isBlank()) {
            throw new IllegalArgumentException("Trajectory workflowId must not be blank");
        }
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public static Trajectory empty(String workflowId) {
        return new Trajectory(workflowId, List.of());
    }

    public Trajectory append(ActionRecord record) {
        Objects.requireNonNull(record, "Cannot append a null ActionRecord");
        List<ActionRecord> next = new ArrayList<>(actions);
        next.add(record);
        return new Trajectory(workflowId, next);
    }
}
