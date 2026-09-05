package com.aios.authz.policy.rules;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionRecord;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.PolicyEvaluation;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.TrustZone;
import com.aios.authz.policy.Policy;
import com.aios.authz.state.AuthorizationState;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The human-adjudicated middle ground {@link ProvenanceBoundaryPolicy}'s
 * binary ALLOW/DENY has no room for: {@code CONFIDENTIAL} (not
 * {@code RESTRICTED}) data reaching a {@code PARTNER} (not {@code EXTERNAL})
 * destination, under an intent that has been explicitly approved for partner
 * disclosure, returns ASK instead of an outright DENY.
 *
 * <p>Resolves the inconsistency flagged in docs/SCENARIOS.md since ENG-19:
 * Scenario D and Scenario H are both "CONFIDENTIAL data reaches PARTNER", and
 * only their {@code Intent} distinguishes them. This is deliberately the sole
 * distinguishing signal — a fixed, explicit allowlist of intent ids, checked
 * here and mirrored by {@link ProvenanceBoundaryPolicy}'s carve-out — not a
 * general-purpose approval mechanism. Scenario E's {@code RESTRICTED} data
 * still denies outright regardless of intent: this narrows only the exact
 * CONFIDENTIAL+PARTNER cell, nothing else.
 *
 * <p>Without this, the only lever against a false positive here would be
 * loosening {@code ProvenanceBoundaryPolicy} itself — which would make kill
 * condition C artificially easy to satisfy by weakening the policy rather than
 * by the policy correctly discriminating cases.
 */
public final class PartnerDisclosureAskPolicy implements Policy {

    /**
     * Deliberately a small, explicit list — not a general "approved" flag on
     * {@code Intent} — since this carve-out is 1G's narrowest possible proof
     * that ASK is reachable, not a real approval workflow.
     */
    static final Set<String> APPROVED_PARTNER_DISCLOSURE_INTENT_IDS = Set.of("approved-partner-disclosure");

    @Override
    public String id() {
        return "partner-disclosure-ask";
    }

    @Override
    public boolean appliesTo(AuthorizationState state, Action action) {
        return action.destination().trustZone() == TrustZone.PARTNER
            && state.provenanceGraph().find(action.resource()).isPresent();
    }

    @Override
    public PolicyEvaluation evaluate(AuthorizationState state, Action action, Principal actingPrincipal) {
        String resource = action.resource();
        Classification effective = state.provenanceGraph().effectiveClassification(resource);

        if (!isEligible(effective, state.intent().id())) {
            return new PolicyEvaluation(
                id(), Decision.ALLOW, "not eligible for the partner-disclosure ASK carve-out", Set.of(), List.of());
        }

        Set<DataAsset> dominant = state.provenanceGraph().dominantRootsOf(resource);
        Set<String> dominantIds = dominant.stream().map(DataAsset::id).collect(Collectors.toUnmodifiableSet());
        List<String> acquiringActionIds = acquiringActionIdsFor(state, dominantIds);

        String reason =
            "%s (effective %s) reaches PARTNER under an approved-disclosure intent; derives from %s — requires human approval, not an automatic decision"
                .formatted(resource, effective, String.join(", ", dominantIds));

        return new PolicyEvaluation(id(), Decision.ASK, reason, dominantIds, acquiringActionIds);
    }

    static boolean isEligible(Classification effective, String intentId) {
        return effective == Classification.CONFIDENTIAL
            && APPROVED_PARTNER_DISCLOSURE_INTENT_IDS.contains(intentId);
    }

    private static List<String> acquiringActionIdsFor(AuthorizationState state, Set<String> rootIds) {
        return state.trajectory().actions().stream()
            .filter(record -> record.decision() == Decision.ALLOW
                && record.action().type() == ActionType.READ
                && rootIds.contains(record.action().resource()))
            .map(ActionRecord::id)
            .toList();
    }
}
