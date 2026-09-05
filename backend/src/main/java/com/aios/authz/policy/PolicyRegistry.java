package com.aios.authz.policy;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.PolicyEvaluation;
import com.aios.authz.state.AuthorizationState;

import java.util.List;
import java.util.Objects;

/**
 * Runs every applicable {@link Policy} against one proposed action. Discovery
 * is registry-based — the caller supplies the full policy list (Spring
 * collects every {@code Policy} bean into it in production) — so adding a rule
 * never means editing this class.
 */
public final class PolicyRegistry {

    private final List<Policy> policies;

    public PolicyRegistry(List<Policy> policies) {
        Objects.requireNonNull(policies, "policies must not be null");
        this.policies = List.copyOf(policies);
    }

    public List<PolicyEvaluation> evaluate(AuthorizationState state, Action action) {
        return policies.stream()
            .filter(policy -> policy.appliesTo(state, action))
            .map(policy -> policy.evaluate(state, action))
            .toList();
    }
}
