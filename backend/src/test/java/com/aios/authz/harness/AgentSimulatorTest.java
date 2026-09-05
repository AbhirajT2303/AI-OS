package com.aios.authz.harness;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.Destination;
import com.aios.authz.domain.DestinationKind;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.PrincipalType;
import com.aios.authz.domain.TrustZone;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AgentSimulatorTest {

    private final Principal agent = new Principal("agent-42", PrincipalType.AGENT);
    private final Destination externalLlm =
        new Destination("openai-api", DestinationKind.MODEL, TrustZone.EXTERNAL);
    private final Destination partnerEmail =
        new Destination("partner.com", DestinationKind.EMAIL, TrustZone.EXTERNAL);

    private TrajectoryAwareEngine newTrajectoryEngine(InMemoryAuthorizationStateStore store) {
        PermissionCatalog permissions = new PermissionCatalog(List.of(
            new PermissionCatalog.Grant("agent-42", ActionType.READ, "*"),
            new PermissionCatalog.Grant("agent-42", ActionType.CALL_MODEL, "*"),
            new PermissionCatalog.Grant("agent-42", ActionType.SEND_EXTERNAL, "*")));
        RbacBaselineEngine rbac = new RbacBaselineEngine(permissions);
        PolicyRegistry registry = new PolicyRegistry(List.of(new ProvenanceBoundaryPolicy()));
        DataAssetCatalog dataAssets = new DataAssetCatalog(List.of(
            new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db")));
        return new TrajectoryAwareEngine(rbac, store, registry, dataAssets);
    }

    @Test
    void replaysScenarioBAndMatchesEachStepsExpectedTrajectoryDecision() {
        InMemoryAuthorizationStateStore store = new InMemoryAuthorizationStateStore();
        WorkflowManager workflowManager = new WorkflowManager(store);
        TrajectoryAwareEngine engine = newTrajectoryEngine(store);
        AgentSimulator simulator = new AgentSimulator(engine, workflowManager);

        Scenario scenario = new ScenarioLoader().load("/scenarios/scenario-b-confidential-external-model.json");
        List<AgentSimulator.StepResult> results = simulator.run(scenario);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(AgentSimulator.StepResult::matchesExpectedTrajectory);
    }

    @Test
    void continuesReplayingAfterADeniedStepRatherThanStoppingEarly() {
        InMemoryAuthorizationStateStore store = new InMemoryAuthorizationStateStore();
        WorkflowManager workflowManager = new WorkflowManager(store);
        TrajectoryAwareEngine engine = newTrajectoryEngine(store);
        AgentSimulator simulator = new AgentSimulator(engine, workflowManager);

        Intent intent = new Intent("test-intent", "Prove replay continues after a deny.");
        ScenarioStep read = new ScenarioStep(
            1, agent,
            new Action("act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42"),
            Decision.ALLOW, Decision.ALLOW);
        ScenarioStep deniedCall = new ScenarioStep(
            2, agent,
            new Action("act-2", ActionType.CALL_MODEL, "customer-42", externalLlm, Set.of("customer-42"), "s1"),
            Decision.ALLOW, Decision.DENY);
        ScenarioStep laterStep = new ScenarioStep(
            3, agent,
            new Action("act-3", ActionType.SEND_EXTERNAL, "s1", partnerEmail, Set.of("s1"), null),
            Decision.ALLOW, Decision.DENY);
        Scenario scenario = new Scenario("continues-after-deny", "test", intent,
            List.of(read, deniedCall, laterStep));

        List<AgentSimulator.StepResult> results = simulator.run(scenario);

        assertThat(results).hasSize(3);
        assertThat(results.get(2).step().step()).isEqualTo(3);
    }
}
