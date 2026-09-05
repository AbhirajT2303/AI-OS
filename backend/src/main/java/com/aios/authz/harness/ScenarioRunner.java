package com.aios.authz.harness;

import com.aios.authz.domain.AuthorizationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs one scenario through both engines and pairs the per-step decisions —
 * the experiment itself. Each simulator opens its own workflow against its own
 * independent state store, so the two runs never share mutable state; "both
 * engines receive byte-identical requests" means identical principal, intent
 * and action per step, not a literally shared {@code workflowId} — sharing one
 * would require sharing a state store, which would let one engine's
 * side-effects leak into the other's decisions.
 */
public final class ScenarioRunner {

    private final AgentSimulator rbacSimulator;
    private final AgentSimulator trajectorySimulator;

    public ScenarioRunner(AgentSimulator rbacSimulator, AgentSimulator trajectorySimulator) {
        this.rbacSimulator = Objects.requireNonNull(rbacSimulator, "rbacSimulator must not be null");
        this.trajectorySimulator = Objects.requireNonNull(trajectorySimulator, "trajectorySimulator must not be null");
    }

    public ComparisonResult run(Scenario scenario) {
        List<AgentSimulator.StepResult> rbacResults = rbacSimulator.run(scenario);
        List<AgentSimulator.StepResult> trajectoryResults = trajectorySimulator.run(scenario);

        List<StepComparison> comparisons = new ArrayList<>();
        for (int i = 0; i < scenario.steps().size(); i++) {
            comparisons.add(new StepComparison(
                scenario.steps().get(i),
                rbacResults.get(i).actual(),
                trajectoryResults.get(i).actual()));
        }
        return new ComparisonResult(scenario, List.copyOf(comparisons));
    }

    /** One step's decision from each engine, each carrying its own measured {@code evaluationTime}. */
    public record StepComparison(ScenarioStep step, AuthorizationResult rbacResult, AuthorizationResult trajectoryResult) {

        public boolean rbacMatchesExpected() {
            return rbacResult.decision() == step.expectedRbac();
        }

        public boolean trajectoryMatchesExpected() {
            return trajectoryResult.decision() == step.expectedTrajectory();
        }

        /** True on exactly the cases the thesis is about: RBAC=ALLOW, trajectory=DENY (or ASK). */
        public boolean isDivergent() {
            return rbacResult.decision() != trajectoryResult.decision();
        }
    }

    public record ComparisonResult(Scenario scenario, List<StepComparison> stepComparisons) {
    }
}
