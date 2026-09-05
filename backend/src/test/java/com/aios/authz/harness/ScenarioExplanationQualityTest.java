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
 * Verifies the canonical scenario (E) produces an explanation meeting all four
 * requirements in docs/SCENARIOS.md, end to end through the real harness — not
 * just that ProvenanceBoundaryPolicy's reason string looks right in isolation
 * (ProvenanceBoundaryPolicyTest already covers that).
 */
class ScenarioExplanationQualityTest {

    private TrajectoryAwareEngine newEngine(InMemoryAuthorizationStateStore store) {
        PermissionCatalog permissions = new PermissionCatalog(List.of(
            new PermissionCatalog.Grant("agent-42", ActionType.READ, "*"),
            new PermissionCatalog.Grant("agent-42", ActionType.TRANSFORM, "*"),
            new PermissionCatalog.Grant("agent-42", ActionType.CALL_MODEL, "*"),
            new PermissionCatalog.Grant("agent-42", ActionType.WRITE, "*"),
            new PermissionCatalog.Grant("agent-42", ActionType.SEND_EXTERNAL, "*")));
        RbacBaselineEngine rbac = new RbacBaselineEngine(permissions);
        PolicyRegistry registry = new PolicyRegistry(List.of(new ProvenanceBoundaryPolicy()));
        DataAssetCatalog dataAssets = new DataAssetCatalog(List.of(
            new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db"),
            new DataAsset("pricing-strategy", Classification.RESTRICTED, "pricing-db"),
            new DataAsset("support-tickets-42", Classification.INTERNAL, "support-db")));
        return new TrajectoryAwareEngine(rbac, store, registry, dataAssets);
    }

    @Test
    void scenarioE_finalSendIsDeniedWithAnExplanationNamingTheDominantContributor() {
        InMemoryAuthorizationStateStore store = new InMemoryAuthorizationStateStore();
        WorkflowManager workflowManager = new WorkflowManager(store);
        TrajectoryAwareEngine engine = newEngine(store);
        AgentSimulator simulator = new AgentSimulator(engine, workflowManager);

        Scenario scenario = new ScenarioLoader().load("/scenarios/scenario-e-multi-source.json");
        List<AgentSimulator.StepResult> results = simulator.run(scenario);

        assertThat(results).hasSize(7);
        assertThat(results).allMatch(AgentSimulator.StepResult::matchesExpectedTrajectory);

        var finalStep = results.get(6);
        assertThat(finalStep.actual().decision()).isEqualTo(Decision.DENY);

        String explanation = finalStep.actual().explanation();
        // 1: names the dominant contributor
        assertThat(explanation).contains("pricing-strategy");
        // 2: names the action that acquired it (step 2 in the scenario's own numbering)
        assertThat(explanation).contains("act-2");
        // 3: states the effective classification and the destination zone maximum
        assertThat(explanation).contains("RESTRICTED").contains("PARTNER").contains("PUBLIC");
        // 4: no data content — the domain model has no content field to leak in
        // the first place; this is structural, not just an absence-of-string check.
        assertThat(explanation).doesNotContain("customer-db").doesNotContain("pricing-db");
    }

    @Test
    void scenarioD_finalSendIsDeniedCitingTheSoleContributor() {
        InMemoryAuthorizationStateStore store = new InMemoryAuthorizationStateStore();
        WorkflowManager workflowManager = new WorkflowManager(store);
        TrajectoryAwareEngine engine = newEngine(store);
        AgentSimulator simulator = new AgentSimulator(engine, workflowManager);

        Scenario scenario = new ScenarioLoader().load("/scenarios/scenario-d-report-external-email.json");
        List<AgentSimulator.StepResult> results = simulator.run(scenario);

        assertThat(results).hasSize(4);
        assertThat(results).allMatch(AgentSimulator.StepResult::matchesExpectedTrajectory);

        var finalStep = results.get(3);
        String explanation = finalStep.actual().explanation();
        assertThat(explanation).contains("customer-42").contains("act-1")
            .contains("CONFIDENTIAL").contains("PARTNER");
    }
}
