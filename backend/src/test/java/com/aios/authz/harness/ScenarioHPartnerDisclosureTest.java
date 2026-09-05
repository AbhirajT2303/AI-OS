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
import com.aios.authz.policy.rules.PartnerDisclosureAskPolicy;
import com.aios.authz.policy.rules.ProvenanceBoundaryPolicy;
import com.aios.authz.state.InMemoryAuthorizationStateStore;
import com.aios.authz.state.WorkflowManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario H exercises ASK, which no other scenario reaches: CONFIDENTIAL data
 * to a PARTNER destination under an intent explicitly approved for partner
 * disclosure. Distinct from Scenario D — same classification, same trust
 * zone, but an unapproved intent — which still denies outright (see
 * ProvenanceBoundaryPolicyTest and ScenarioExplanationQualityTest).
 */
class ScenarioHPartnerDisclosureTest {

    private ScenarioRunner newRunner() {
        PermissionCatalog permissions = new PermissionCatalog(List.of(
            new PermissionCatalog.Grant("agent-42", ActionType.READ, "*"),
            new PermissionCatalog.Grant("agent-42", ActionType.WRITE, "*"),
            new PermissionCatalog.Grant("agent-42", ActionType.SEND_EXTERNAL, "*")));
        DataAssetCatalog dataAssets = new DataAssetCatalog(List.of(
            new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db")));

        AgentSimulator rbacSimulator = new AgentSimulator(
            new RbacBaselineEngine(permissions), new WorkflowManager(new InMemoryAuthorizationStateStore()));

        InMemoryAuthorizationStateStore trajectoryStore = new InMemoryAuthorizationStateStore();
        TrajectoryAwareEngine trajectoryEngine = new TrajectoryAwareEngine(
            new RbacBaselineEngine(permissions),
            trajectoryStore,
            new PolicyRegistry(List.of(new ProvenanceBoundaryPolicy(), new PartnerDisclosureAskPolicy())),
            dataAssets);
        AgentSimulator trajectorySimulator = new AgentSimulator(trajectoryEngine, new WorkflowManager(trajectoryStore));

        return new ScenarioRunner(rbacSimulator, trajectorySimulator);
    }

    @Test
    void finalSendAsksRatherThanDeniesOrAllows() {
        Scenario scenario = new ScenarioLoader().load("/scenarios/scenario-h-partner-disclosure.json");

        ScenarioRunner.ComparisonResult result = newRunner().run(scenario);

        assertThat(result.stepComparisons()).hasSize(3);
        assertThat(result.stepComparisons()).allMatch(ScenarioRunner.StepComparison::rbacMatchesExpected);
        assertThat(result.stepComparisons()).allMatch(ScenarioRunner.StepComparison::trajectoryMatchesExpected);

        ScenarioRunner.StepComparison finalStep = result.stepComparisons().get(2);
        assertThat(finalStep.rbacResult().decision()).isEqualTo(Decision.ALLOW);
        assertThat(finalStep.trajectoryResult().decision()).isEqualTo(Decision.ASK);
    }
}
