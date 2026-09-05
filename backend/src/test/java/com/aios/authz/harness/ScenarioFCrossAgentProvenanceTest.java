package com.aios.authz.harness;

import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.Decision;
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

/**
 * Scenario F: agent-C never touches customer-db and holds a valid
 * SEND_EXTERNAL permission — this is the case existing per-request systems
 * are least able to express, since no single request agent-C makes contains
 * the evidence for why it should be denied.
 */
class ScenarioFCrossAgentProvenanceTest {

    // Only agent-C's own grant, proving the eventual DENY cannot be explained
    // by a permission gap for agent-C — it never has one.
    private static final PermissionCatalog AGENT_C_ONLY_PERMISSIONS = new PermissionCatalog(List.of(
        new PermissionCatalog.Grant("agent-A", ActionType.READ, "*"),
        new PermissionCatalog.Grant("agent-A", ActionType.DELEGATE, "*"),
        new PermissionCatalog.Grant("agent-B", ActionType.CALL_MODEL, "*"),
        new PermissionCatalog.Grant("agent-B", ActionType.DELEGATE, "*"),
        new PermissionCatalog.Grant("agent-C", ActionType.SEND_EXTERNAL, "*")));

    private ScenarioRunner newRunner() {
        AgentSimulator rbacSimulator = new AgentSimulator(
            new RbacBaselineEngine(AGENT_C_ONLY_PERMISSIONS),
            new WorkflowManager(new InMemoryAuthorizationStateStore()));

        InMemoryAuthorizationStateStore trajectoryStore = new InMemoryAuthorizationStateStore();
        TrajectoryAwareEngine trajectoryEngine = new TrajectoryAwareEngine(
            new RbacBaselineEngine(AGENT_C_ONLY_PERMISSIONS),
            trajectoryStore,
            new PolicyRegistry(List.of(new ProvenanceBoundaryPolicy())),
            new DataAssetCatalog(List.of(new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db"))));
        AgentSimulator trajectorySimulator = new AgentSimulator(trajectoryEngine, new WorkflowManager(trajectoryStore));

        return new ScenarioRunner(rbacSimulator, trajectorySimulator);
    }

    @Test
    void everyStepMatchesItsExpectedDecisionWithOnlyEachAgentsOwnMinimalGrant() {
        Scenario scenario = new ScenarioLoader().load("/scenarios/scenario-f-multi-agent.json");

        ScenarioRunner.ComparisonResult result = newRunner().run(scenario);

        assertThat(result.stepComparisons()).hasSize(5);
        assertThat(result.stepComparisons()).allMatch(ScenarioRunner.StepComparison::rbacMatchesExpected);
        assertThat(result.stepComparisons()).allMatch(ScenarioRunner.StepComparison::trajectoryMatchesExpected);
    }

    @Test
    void divergesOnlyAtAgentCsFinalSend_provingTheDenialIsNotAPermissionGap() {
        Scenario scenario = new ScenarioLoader().load("/scenarios/scenario-f-multi-agent.json");

        ScenarioRunner.ComparisonResult result = newRunner().run(scenario);

        for (int i = 0; i < 4; i++) {
            assertThat(result.stepComparisons().get(i).isDivergent())
                .as("step %d should not diverge", i + 1)
                .isFalse();
        }

        ScenarioRunner.StepComparison finalStep = result.stepComparisons().get(4);
        assertThat(finalStep.step().actor().id()).isEqualTo("agent-C");
        // The permission catalog used here grants agent-C nothing but
        // SEND_EXTERNAL — RBAC still ALLOWs, proving the permission genuinely
        // exists, not that the baseline was built weak.
        assertThat(finalStep.rbacResult().decision()).isEqualTo(Decision.ALLOW);
        assertThat(finalStep.trajectoryResult().decision()).isEqualTo(Decision.DENY);
        assertThat(finalStep.isDivergent()).isTrue();

        // ENG-29: the explanation names both agent-A, who acquired customer-42
        // three steps earlier, and agent-C, who is attempting the send now —
        // distinct principals, neither of which the naive single-request view
        // agent-C's own call presents on its own.
        assertThat(finalStep.trajectoryResult().explanation()).contains("agent-A").contains("agent-C");
    }
}
