package com.aios.authz.harness;

import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.engine.DataAssetCatalog;
import com.aios.authz.engine.PermissionCatalog;
import com.aios.authz.engine.RbacBaselineEngine;
import com.aios.authz.engine.TrajectoryAwareEngine;
import com.aios.authz.policy.PolicyRegistry;
import com.aios.authz.policy.rules.ProvenanceBoundaryPolicy;
import com.aios.authz.state.InMemoryAuthorizationStateStore;
import com.aios.authz.state.WorkflowManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioRunnerTest {

    private static final PermissionCatalog WIDE_OPEN_PERMISSIONS = new PermissionCatalog(List.of(
        new PermissionCatalog.Grant("agent-42", ActionType.READ, "*"),
        new PermissionCatalog.Grant("agent-42", ActionType.TRANSFORM, "*"),
        new PermissionCatalog.Grant("agent-42", ActionType.CALL_MODEL, "*"),
        new PermissionCatalog.Grant("agent-42", ActionType.WRITE, "*"),
        new PermissionCatalog.Grant("agent-42", ActionType.SEND_EXTERNAL, "*")));

    private static final List<DataAsset> SCENARIO_E_ASSETS = List.of(
        new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db"),
        new DataAsset("pricing-strategy", Classification.RESTRICTED, "pricing-db"),
        new DataAsset("support-tickets-42", Classification.INTERNAL, "support-db"));

    private ScenarioRunner newRunner() {
        AgentSimulator rbacSimulator = new AgentSimulator(
            new RbacBaselineEngine(WIDE_OPEN_PERMISSIONS),
            new WorkflowManager(new InMemoryAuthorizationStateStore()));

        InMemoryAuthorizationStateStore trajectoryStore = new InMemoryAuthorizationStateStore();
        TrajectoryAwareEngine trajectoryEngine = new TrajectoryAwareEngine(
            new RbacBaselineEngine(WIDE_OPEN_PERMISSIONS),
            trajectoryStore,
            new PolicyRegistry(List.of(new ProvenanceBoundaryPolicy())),
            new DataAssetCatalog(SCENARIO_E_ASSETS));
        AgentSimulator trajectorySimulator = new AgentSimulator(
            trajectoryEngine, new WorkflowManager(trajectoryStore));

        return new ScenarioRunner(rbacSimulator, trajectorySimulator);
    }

    @Test
    void scenarioE_everyStepMatchesItsExpectedDecisionOnBothEngines() {
        Scenario scenario = new ScenarioLoader().load("/scenarios/scenario-e-multi-source.json");

        ScenarioRunner.ComparisonResult result = newRunner().run(scenario);

        assertThat(result.stepComparisons()).hasSize(7);
        assertThat(result.stepComparisons()).allMatch(ScenarioRunner.StepComparison::rbacMatchesExpected);
        assertThat(result.stepComparisons()).allMatch(ScenarioRunner.StepComparison::trajectoryMatchesExpected);
    }

    @Test
    void scenarioE_divergesOnlyAtTheFinalExternalSend() {
        Scenario scenario = new ScenarioLoader().load("/scenarios/scenario-e-multi-source.json");

        ScenarioRunner.ComparisonResult result = newRunner().run(scenario);

        for (int i = 0; i < 6; i++) {
            assertThat(result.stepComparisons().get(i).isDivergent())
                .as("step %d should not diverge", i + 1)
                .isFalse();
        }
        assertThat(result.stepComparisons().get(6).isDivergent())
            .as("step 7 (the final SEND_EXTERNAL) is the thesis-relevant divergence")
            .isTrue();
    }
}
