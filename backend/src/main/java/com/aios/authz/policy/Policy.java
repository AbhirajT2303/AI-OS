package com.aios.authz.policy;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.PolicyEvaluation;
import com.aios.authz.state.AuthorizationState;

/**
 * One rule, evaluated independently of every other. Stateless and
 * side-effect-free so each implementation is testable in isolation and adding
 * one never requires editing {@link PolicyRegistry} or an engine.
 */
public interface Policy {

    String id();

    /** False lets a policy abstain from an evaluations list rather than return a vacuous ALLOW. */
    boolean appliesTo(AuthorizationState state, Action action);

    PolicyEvaluation evaluate(AuthorizationState state, Action action);
}
