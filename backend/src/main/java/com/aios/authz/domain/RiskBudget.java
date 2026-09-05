package com.aios.authz.domain;

/**
 * A workflow's accumulated impact against a fixed ceiling. Not stored on
 * {@code AuthorizationState} — {@code RiskBudgetPolicy} computes one on demand
 * from the trajectory each time, the same derived-view pattern
 * {@code AuthorizationState.heldAssets()} already uses, rather than adding
 * another stored field that every direct state construction would need to
 * carry.
 */
public record RiskBudget(int total, int spent) {

    public RiskBudget {
        if (total < 0) {
            throw new IllegalArgumentException("RiskBudget total must not be negative");
        }
        if (spent < 0) {
            throw new IllegalArgumentException("RiskBudget spent must not be negative");
        }
    }

    public int remaining() {
        return total - spent;
    }
}
