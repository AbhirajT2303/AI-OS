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
 * Denies when the proposed action's own resource has an
 * <em>effective</em> — lineage-derived, not merely declared — classification
 * exceeding the maximum its destination's trust zone allows. This is the
 * precise successor to {@link ConfidentialToExternalPolicy} and
 * {@link ModelBoundaryPolicy}: those ask "is any confidential-or-above data
 * held anywhere in this workflow", which denies a safe send of unrelated
 * public data merely because something sensitive was read earlier in the same
 * workflow. This asks "is *this* resource, specifically, derived from
 * something too sensitive for *this* destination" — the reasoning
 * Scenarios D and E need, and the reasoning that avoids that false positive.
 *
 * <p>Applicable to any action with a resource the graph already knows about;
 * an unknown resource (nothing yet derived or read under that id) has nothing
 * to complain about and ALLOWs.
 */
public final class ProvenanceBoundaryPolicy implements Policy {

    @Override
    public String id() {
        return "provenance-boundary";
    }

    @Override
    public boolean appliesTo(AuthorizationState state, Action action) {
        return state.provenanceGraph().find(action.resource()).isPresent();
    }

    @Override
    public PolicyEvaluation evaluate(AuthorizationState state, Action action, Principal actingPrincipal) {
        String resource = action.resource();
        Classification effective = state.provenanceGraph().effectiveClassification(resource);
        Classification max = maxAllowedFor(action.destination().trustZone());

        if (max.atLeast(effective)) {
            return new PolicyEvaluation(
                id(), Decision.ALLOW,
                "%s (effective %s) does not exceed %s zone maximum %s"
                    .formatted(resource, effective, action.destination().trustZone(), max),
                Set.of(), List.of());
        }

        Set<DataAsset> dominant = state.provenanceGraph().dominantRootsOf(resource);
        Set<String> dominantIds = dominant.stream().map(DataAsset::id).collect(Collectors.toUnmodifiableSet());
        List<ActionRecord> acquiringRecords = acquiringRecordsFor(state, dominantIds);
        List<String> acquiringActionIds = acquiringRecords.stream().map(ActionRecord::id).toList();

        String acquirers = acquiringRecords.stream()
            .map(record -> "%s at %s".formatted(record.actor().id(), record.id()))
            .collect(Collectors.joining(", "));

        String reason =
            "%s exceeds %s zone maximum %s: effective classification %s derives from %s, acquired by %s; %s now attempts to send it"
                .formatted(
                    resource, action.destination().trustZone(), max, effective,
                    String.join(", ", dominantIds), acquirers, actingPrincipal.id());

        return new PolicyEvaluation(id(), Decision.DENY, reason, dominantIds, acquiringActionIds);
    }

    private static List<ActionRecord> acquiringRecordsFor(AuthorizationState state, Set<String> rootIds) {
        return state.trajectory().actions().stream()
            .filter(record -> record.decision() == Decision.ALLOW
                && record.action().type() == ActionType.READ
                && rootIds.contains(record.action().resource()))
            .toList();
    }

    /**
     * The most sensitive classification a destination's trust zone may
     * silently receive. INTERNAL allows everything (RESTRICTED, the ceiling);
     * PARTNER and EXTERNAL only silently allow PUBLIC — anything above that
     * either denies here or, for PARTNER specifically, is a candidate for a
     * human-adjudicated ASK once {@code PartnerDisclosureAskPolicy} (ENG-41,
     * Sprint 4) exists. See docs/SCENARIOS.md's open Scenario D/H
     * inconsistency note for why that carve-out isn't implemented yet.
     */
    private static Classification maxAllowedFor(TrustZone zone) {
        return switch (zone) {
            case INTERNAL -> Classification.RESTRICTED;
            case PARTNER, EXTERNAL -> Classification.PUBLIC;
        };
    }
}
