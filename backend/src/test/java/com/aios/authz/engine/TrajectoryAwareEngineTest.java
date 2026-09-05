package com.aios.authz.engine;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.AuthorizationRequest;
import com.aios.authz.domain.AuthorizationResult;
import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.Destination;
import com.aios.authz.domain.DestinationKind;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.PrincipalType;
import com.aios.authz.domain.TrustZone;
import com.aios.authz.policy.PolicyRegistry;
import com.aios.authz.policy.rules.ProvenanceBoundaryPolicy;
import com.aios.authz.state.AuthorizationState;
import com.aios.authz.state.InMemoryAuthorizationStateStore;
import com.aios.authz.state.UnknownWorkflowException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrajectoryAwareEngineTest {

    private final Principal agent42 = new Principal("agent-42", PrincipalType.AGENT);
    private final Intent intent = new Intent("partner-report", "Prepare a report for a partner.");

    private final Destination internalLlm =
        new Destination("internal-llm", DestinationKind.MODEL, TrustZone.INTERNAL);
    private final Destination externalLlm =
        new Destination("openai-api", DestinationKind.MODEL, TrustZone.EXTERNAL);

    private TrajectoryAwareEngine newEngine(InMemoryAuthorizationStateStore store) {
        PermissionCatalog catalog = new PermissionCatalog(List.of(
            new PermissionCatalog.Grant("agent-42", ActionType.READ, "*"),
            new PermissionCatalog.Grant("agent-42", ActionType.CALL_MODEL, "*"),
            new PermissionCatalog.Grant("agent-42", ActionType.SEND_EXTERNAL, "*")));
        RbacBaselineEngine rbac = new RbacBaselineEngine(catalog);
        PolicyRegistry registry = new PolicyRegistry(List.of(new ProvenanceBoundaryPolicy()));
        DataAssetCatalog dataAssets = new DataAssetCatalog(List.of(
            new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db"),
            new DataAsset("public-brochure", Classification.PUBLIC, "marketing-cms")));
        return new TrajectoryAwareEngine(rbac, store, registry, dataAssets);
    }

    @Test
    void unknownWorkflowThrowsRatherThanImplicitlyOpeningOne() {
        TrajectoryAwareEngine engine = newEngine(new InMemoryAuthorizationStateStore());
        Action read = new Action(
            "act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42");

        assertThatThrownBy(() -> engine.authorize(
            new AuthorizationRequest("wf-never-opened", agent42, intent, read)))
            .isInstanceOf(UnknownWorkflowException.class);
    }

    @Test
    void scenarioB_readConfidentialThenCallExternalModel_deniesAfterAllowingTheRead() {
        InMemoryAuthorizationStateStore store = new InMemoryAuthorizationStateStore();
        store.save(AuthorizationState.open("wf-1", agent42, intent));
        TrajectoryAwareEngine engine = newEngine(store);

        Action read = new Action(
            "act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42");
        AuthorizationResult readResult = engine.authorize(
            new AuthorizationRequest("wf-1", agent42, intent, read));
        assertThat(readResult.decision()).isEqualTo(Decision.ALLOW);

        Action callExternal = new Action(
            "act-2", ActionType.CALL_MODEL, "customer-42", externalLlm, Set.of("customer-42"), "summary-1");
        AuthorizationResult callResult = engine.authorize(
            new AuthorizationRequest("wf-1", agent42, intent, callExternal));

        assertThat(callResult.decision()).isEqualTo(Decision.DENY);
    }

    @Test
    void scenarioC_readConfidentialThenCallInternalModel_allows() {
        InMemoryAuthorizationStateStore store = new InMemoryAuthorizationStateStore();
        store.save(AuthorizationState.open("wf-1", agent42, intent));
        TrajectoryAwareEngine engine = newEngine(store);

        Action read = new Action(
            "act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42");
        engine.authorize(new AuthorizationRequest("wf-1", agent42, intent, read));

        Action callInternal = new Action(
            "act-2", ActionType.CALL_MODEL, "customer-42", internalLlm, Set.of("customer-42"), "summary-1");
        AuthorizationResult callResult = engine.authorize(
            new AuthorizationRequest("wf-1", agent42, intent, callInternal));

        assertThat(callResult.decision()).isEqualTo(Decision.ALLOW);
    }

    @Test
    void neverConvertsAnRbacDenyIntoAllow() {
        InMemoryAuthorizationStateStore store = new InMemoryAuthorizationStateStore();
        store.save(AuthorizationState.open("wf-1", agent42, intent));
        // No grants at all in this catalog, so RBAC denies everything.
        RbacBaselineEngine noPermissions = new RbacBaselineEngine(new PermissionCatalog(List.of()));
        PolicyRegistry emptyRegistry = new PolicyRegistry(List.of());
        DataAssetCatalog dataAssets = new DataAssetCatalog(List.of());
        TrajectoryAwareEngine engine = new TrajectoryAwareEngine(noPermissions, store, emptyRegistry, dataAssets);

        Action read = new Action(
            "act-1", ActionType.READ, "public-brochure", Destination.NONE, Set.of(), "public-brochure");
        AuthorizationResult result = engine.authorize(new AuthorizationRequest("wf-1", agent42, intent, read));

        assertThat(result.decision()).isEqualTo(Decision.DENY);
    }

    @Test
    void allowedReadIsRecordedInTrajectoryAndAddsToHeldAssets() {
        InMemoryAuthorizationStateStore store = new InMemoryAuthorizationStateStore();
        store.save(AuthorizationState.open("wf-1", agent42, intent));
        TrajectoryAwareEngine engine = newEngine(store);

        Action read = new Action(
            "act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42");
        engine.authorize(new AuthorizationRequest("wf-1", agent42, intent, read));

        AuthorizationState state = store.find("wf-1").orElseThrow();
        assertThat(state.heldAssets()).extracting(DataAsset::id).containsExactly("customer-42");
        assertThat(state.trajectory().actions()).hasSize(1);
    }
}
