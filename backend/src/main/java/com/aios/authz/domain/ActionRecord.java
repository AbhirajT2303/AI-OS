package com.aios.authz.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * One entry in a {@link Trajectory}: an action a principal proposed, and the
 * decision it received. Denied and asked actions are recorded here too, not
 * only allowed ones — an agent retrying a denied action along a different path
 * is itself a signal 1H will attack.
 */
public record ActionRecord(String id, Principal actor, Action action, Decision decision, Instant timestamp) {

    public ActionRecord {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ActionRecord id must not be blank");
        }
        Objects.requireNonNull(actor, "ActionRecord actor must not be null");
        Objects.requireNonNull(action, "ActionRecord action must not be null");
        Objects.requireNonNull(decision, "ActionRecord decision must not be null");
        Objects.requireNonNull(timestamp, "ActionRecord timestamp must not be null");
    }
}
