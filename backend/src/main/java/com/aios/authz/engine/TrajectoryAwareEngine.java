package com.aios.authz.engine;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionRecord;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.AuthorizationRequest;
import com.aios.authz.domain.AuthorizationResult;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.PolicyEvaluation;
import com.aios.authz.policy.DecisionCombiner;
import com.aios.authz.policy.PolicyRegistry;
import com.aios.authz.state.AuthorizationState;
import com.aios.authz.state.AuthorizationStateStore;
import com.aios.authz.state.StateTransition;
import com.aios.authz.state.UnknownWorkflowException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The experiment: loads workflow state, runs an RBAC precheck, then every
 * applicable policy, combines by deny-overrides, and commits the resulting
 * state transition on ALLOW.
 *
 * <p>Never converts an RBAC DENY into ALLOW — if the baseline denies, that is
 * the final decision and the policy registry does not even run. This is what
 * keeps every DENY this engine adds beyond the baseline attributable to
 * trajectory, not to some accidental relaxation of RBAC.
 */
public final class TrajectoryAwareEngine implements AuthorizationEngine {

    private final RbacBaselineEngine rbacBaseline;
    private final AuthorizationStateStore stateStore;
    private final PolicyRegistry policyRegistry;
    private final DataAssetCatalog dataAssetCatalog;

    public TrajectoryAwareEngine(
            RbacBaselineEngine rbacBaseline,
            AuthorizationStateStore stateStore,
            PolicyRegistry policyRegistry,
            DataAssetCatalog dataAssetCatalog) {
        this.rbacBaseline = Objects.requireNonNull(rbacBaseline, "rbacBaseline must not be null");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore must not be null");
        this.policyRegistry = Objects.requireNonNull(policyRegistry, "policyRegistry must not be null");
        this.dataAssetCatalog = Objects.requireNonNull(dataAssetCatalog, "dataAssetCatalog must not be null");
    }

    @Override
    public String name() {
        return "trajectory-aware";
    }

    @Override
    public AuthorizationResult authorize(AuthorizationRequest request) {
        long startNanos = System.nanoTime();

        AuthorizationState state = stateStore.find(request.workflowId())
            .orElseThrow(() -> new UnknownWorkflowException(request.workflowId()));

        AuthorizationResult rbacResult = rbacBaseline.authorize(request);
        List<PolicyEvaluation> evaluations = new ArrayList<>(rbacResult.evaluations());

        Decision combined;
        if (rbacResult.decision() == Decision.DENY) {
            combined = Decision.DENY;
        } else {
            evaluations.addAll(policyRegistry.evaluate(state, request.requestedAction()));
            combined = DecisionCombiner.combine(evaluations);
        }

        String explanation = ExplanationBuilder.build(combined, evaluations);
        AuthorizationState nextState = commitTransition(state, request, combined);
        stateStore.save(nextState);

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        return new AuthorizationResult(combined, explanation, evaluations, elapsed);
    }

    private AuthorizationState commitTransition(
            AuthorizationState state, AuthorizationRequest request, Decision combined) {
        Action action = request.requestedAction();
        ActionRecord record = new ActionRecord(action.id(), request.principal(), action, combined, Instant.now());

        DataAsset resolvedAsset = (combined == Decision.ALLOW && action.type() == ActionType.READ)
            ? dataAssetCatalog.find(action.resource()).orElse(null)
            : null;

        return StateTransition.apply(state, record, resolvedAsset);
    }
}
