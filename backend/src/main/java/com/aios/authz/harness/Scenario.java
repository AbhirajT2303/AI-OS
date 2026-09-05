package com.aios.authz.harness;

import com.aios.authz.domain.Intent;

import java.util.List;
import java.util.Objects;

/**
 * A named sequence of steps run through both engines on identical input — see
 * docs/SCENARIOS.md. All steps share one workflow, opened once under
 * {@code intent}: this is what lets a multi-agent scenario (Scenario F) work
 * without a dedicated cross-agent delegation mechanism — the state is scoped
 * to the workflow, not to whichever principal is acting in a given step.
 */
public record Scenario(String id, String description, Intent intent, List<ScenarioStep> steps) {

    public Scenario {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Scenario id must not be blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Scenario description must not be blank");
        }
        Objects.requireNonNull(intent, "Scenario intent must not be null");
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Scenario '" + id + "' must declare at least one step");
        }
        steps = List.copyOf(steps);
    }
}
