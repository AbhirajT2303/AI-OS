package com.aios.authz.policy.rules;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.PolicyEvaluation;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.RiskBudget;
import com.aios.authz.policy.Policy;
import com.aios.authz.state.AuthorizationState;

import java.util.List;
import java.util.Set;

/**
 * A per-workflow impact ceiling, irreversible actions costing more than
 * reversible ones. Exhausting the budget returns ASK, not DENY — a workflow
 * that has done a lot needs a human to keep going, not an automatic refusal.
 *
 * <p>The budget is never stored: it is recomputed each time from the sum of
 * every ALLOWed action's cost in {@code state.trajectory()} — see
 * {@link RiskBudget}'s own note on why.
 */
public final class RiskBudgetPolicy implements Policy {

    static final int TOTAL_BUDGET = 100;

    @Override
    public String id() {
        return "risk-budget";
    }

    @Override
    public boolean appliesTo(AuthorizationState state, Action action) {
        return true;
    }

    @Override
    public PolicyEvaluation evaluate(AuthorizationState state, Action action, Principal actingPrincipal) {
        RiskBudget budget = currentBudget(state);
        int cost = costOf(action.type());

        if (cost > budget.remaining()) {
            String reason = "%d of %d risk budget remaining; this %s costs %d — requires human approval"
                .formatted(budget.remaining(), budget.total(), action.type(), cost);
            return new PolicyEvaluation(id(), Decision.ASK, reason, Set.of(), List.of());
        }

        String reason = "%d of %d risk budget remaining after this action"
            .formatted(budget.remaining() - cost, budget.total());
        return new PolicyEvaluation(id(), Decision.ALLOW, reason, Set.of(), List.of());
    }

    private static RiskBudget currentBudget(AuthorizationState state) {
        int spent = state.trajectory().actions().stream()
            .filter(record -> record.decision() == Decision.ALLOW)
            .mapToInt(record -> costOf(record.action().type()))
            .sum();
        return new RiskBudget(TOTAL_BUDGET, spent);
    }

    /**
     * A simple, fixed cost per action type — irreversible or externally
     * visible actions (SEND_EXTERNAL, DELETE) cost the most; read-only or
     * purely internal actions cost the least.
     */
    private static int costOf(ActionType type) {
        return switch (type) {
            case READ, CALL_TOOL, TRANSFORM -> 1;
            case CALL_MODEL, DELEGATE -> 2;
            case WRITE -> 3;
            case SEND_EXTERNAL, DELETE -> 5;
        };
    }
}
