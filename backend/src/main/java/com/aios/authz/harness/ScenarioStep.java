package com.aios.authz.harness;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.Principal;

import java.util.Objects;

/**
 * One step of a {@link Scenario}: an actor proposing an action, and the
 * decision each engine is expected to produce for it. Both domain types
 * ({@link Principal}, {@link Action}) already validate on construction, so
 * Jackson deserializes them directly — a malformed step fails loudly the same
 * way a malformed fixture does (ENG-03).
 */
public record ScenarioStep(
        int step,
        Principal actor,
        Action action,
        Decision expectedRbac,
        Decision expectedTrajectory) {

    public ScenarioStep {
        if (step <= 0) {
            throw new IllegalArgumentException("ScenarioStep step number must be positive, was: " + step);
        }
        Objects.requireNonNull(actor, "ScenarioStep actor must not be null");
        Objects.requireNonNull(action, "ScenarioStep action must not be null");
        Objects.requireNonNull(expectedRbac, "ScenarioStep expectedRbac must not be null");
        Objects.requireNonNull(expectedTrajectory, "ScenarioStep expectedTrajectory must not be null");
    }
}
