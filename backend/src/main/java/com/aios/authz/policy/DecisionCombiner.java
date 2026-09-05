package com.aios.authz.policy;

import com.aios.authz.domain.Decision;
import com.aios.authz.domain.PolicyEvaluation;

import java.util.List;

/**
 * Deny-overrides: any DENY wins outright; otherwise any ASK wins; otherwise
 * ALLOW. The only safe default — an accidental silent ALLOW is the one outcome
 * that must never happen — and it makes policy composition order-independent,
 * which matters as rules are added in 1H.
 */
public final class DecisionCombiner {

    private DecisionCombiner() {
    }

    public static Decision combine(List<PolicyEvaluation> evaluations) {
        boolean anyDeny = evaluations.stream().anyMatch(e -> e.decision() == Decision.DENY);
        if (anyDeny) {
            return Decision.DENY;
        }
        boolean anyAsk = evaluations.stream().anyMatch(e -> e.decision() == Decision.ASK);
        if (anyAsk) {
            return Decision.ASK;
        }
        return Decision.ALLOW;
    }
}
