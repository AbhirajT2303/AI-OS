package com.aios.authz.policy.rules;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.PolicyEvaluation;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.TrustZone;
import com.aios.authz.policy.Policy;
import com.aios.authz.policy.rules.ConfidentialDataEvidence.Evidence;
import com.aios.authz.state.AuthorizationState;

import java.util.List;
import java.util.Set;

/**
 * A CALL_MODEL to an external model is a boundary crossing for
 * confidential-or-above data, same as SEND_EXTERNAL — this is the fix for
 * GAP-1: an internal and an external CALL_MODEL now differ only by
 * {@code destination.trustZone()}, so this policy can tell them apart where the
 * original sketch could not (Scenario B denies, Scenario C allows).
 *
 * <p>The classification check matters as much as the trust-zone check: Scenario
 * A also calls an external model, and must still ALLOW because its data is
 * PUBLIC.
 */
public final class ModelBoundaryPolicy implements Policy {

    @Override
    public String id() {
        return "model-boundary";
    }

    @Override
    public boolean appliesTo(AuthorizationState state, Action action) {
        return action.type() == ActionType.CALL_MODEL;
    }

    @Override
    public PolicyEvaluation evaluate(AuthorizationState state, Action action, Principal actingPrincipal) {
        if (action.destination().trustZone() != TrustZone.EXTERNAL) {
            return new PolicyEvaluation(
                id(), Decision.ALLOW, "model destination is not external", Set.of(), List.of());
        }

        Evidence evidence = ConfidentialDataEvidence.confidentialOrAboveHeldIn(state);

        if (evidence.isEmpty()) {
            return new PolicyEvaluation(
                id(), Decision.ALLOW, "no confidential-or-above data held", Set.of(), List.of());
        }

        String reason =
            "workflow holds confidential-or-above data (%s) acquired at %s; may not reach an external model"
                .formatted(String.join(", ", evidence.dataIds()), String.join(", ", evidence.actionIds()));

        return new PolicyEvaluation(id(), Decision.DENY, reason, evidence.dataIds(), evidence.actionIds());
    }
}
