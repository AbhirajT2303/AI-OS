package com.aios.authz.policy.rules;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.PolicyEvaluation;
import com.aios.authz.policy.Policy;
import com.aios.authz.policy.rules.ConfidentialDataEvidence.Evidence;
import com.aios.authz.state.AuthorizationState;

import java.util.List;
import java.util.Set;

/**
 * Confidential-or-above data may not reach an external destination via
 * SEND_EXTERNAL. This is intentionally the simple, non-lineage-aware version
 * from PROJECT_CONTEXT.md §18 — not claimed as novel, DLP/data-governance
 * systems can already implement it. It exists as the correctness baseline
 * Scenario D's real (lineage-driven) denial in ENG-25 will be compared against.
 */
public final class ConfidentialToExternalPolicy implements Policy {

    @Override
    public String id() {
        return "confidential-to-external";
    }

    @Override
    public boolean appliesTo(AuthorizationState state, Action action) {
        return action.type() == ActionType.SEND_EXTERNAL;
    }

    @Override
    public PolicyEvaluation evaluate(AuthorizationState state, Action action) {
        Evidence evidence = ConfidentialDataEvidence.confidentialOrAboveHeldIn(state);

        if (evidence.isEmpty()) {
            return new PolicyEvaluation(
                id(), Decision.ALLOW, "no confidential-or-above data held", Set.of(), List.of());
        }

        String reason = "workflow holds confidential-or-above data (%s) acquired at %s; may not send externally"
            .formatted(String.join(", ", evidence.dataIds()), String.join(", ", evidence.actionIds()));

        return new PolicyEvaluation(id(), Decision.DENY, reason, evidence.dataIds(), evidence.actionIds());
    }
}
