package com.aios.authz.harness;

import com.aios.authz.domain.AuthorizationRequest;
import com.aios.authz.domain.AuthorizationResult;
import com.aios.authz.engine.AuthorizationEngine;
import com.aios.authz.state.WorkflowManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Replays a {@link Scenario}'s steps as {@link AuthorizationRequest}s against
 * one engine. Deterministic — no randomness, no LLM.
 *
 * <p>Opens exactly one fresh workflow per run and authorizes every step
 * against it, regardless of which actor performs it, matching how
 * {@link Scenario} is documented: this is what supports a multi-principal
 * scenario (Scenario F) with no separate delegation-tracking mechanism.
 *
 * <p>Continues after a DENY — stopping early would hide exactly the behaviour
 * under study, since a later step's decision (and whether the engine still
 * denies correctly once it has already recorded a refusal) is itself part of
 * what the experiment observes.
 */
public final class AgentSimulator {

    private final AuthorizationEngine engine;
    private final WorkflowManager workflowManager;

    public AgentSimulator(AuthorizationEngine engine, WorkflowManager workflowManager) {
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
        this.workflowManager = Objects.requireNonNull(workflowManager, "workflowManager must not be null");
    }

    public List<StepResult> run(Scenario scenario) {
        String workflowId = workflowManager.open(scenario.steps().get(0).actor(), scenario.intent());

        List<StepResult> results = new ArrayList<>();
        for (ScenarioStep step : scenario.steps()) {
            AuthorizationRequest request =
                new AuthorizationRequest(workflowId, step.actor(), scenario.intent(), step.action());
            AuthorizationResult actual = engine.authorize(request);
            results.add(new StepResult(step, actual));
        }
        return results;
    }

    /**
     * {@code actual} came from whichever engine was passed to this simulator —
     * the caller (e.g. {@code ScenarioRunner}, ENG-33) knows which one and
     * should check the matching expectation, not both.
     */
    public record StepResult(ScenarioStep step, AuthorizationResult actual) {

        public boolean matchesExpectedRbac() {
            return actual.decision() == step.expectedRbac();
        }

        public boolean matchesExpectedTrajectory() {
            return actual.decision() == step.expectedTrajectory();
        }
    }
}
