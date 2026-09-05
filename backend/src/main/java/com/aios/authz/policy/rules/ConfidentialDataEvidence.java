package com.aios.authz.policy.rules;

import com.aios.authz.domain.ActionRecord;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.Decision;
import com.aios.authz.state.AuthorizationState;

import java.util.List;
import java.util.Set;

/**
 * Shared by {@link ConfidentialToExternalPolicy} and {@link ModelBoundaryPolicy}:
 * both deny on "any confidential-or-above data currently held", and both need to
 * cite which data and which prior READ acquired it.
 *
 * <p>Sprint 2 scope note: this looks at {@code heldAssets} as a flat set, not a
 * provenance graph — coarser than "is the thing being sent now derived from
 * this data", but it produces the same ALLOW/DENY split for every scenario that
 * matters before ENG-25 (Sprint 3) makes the reasoning lineage-precise.
 */
final class ConfidentialDataEvidence {

    private ConfidentialDataEvidence() {
    }

    record Evidence(Set<String> dataIds, List<String> actionIds) {

        boolean isEmpty() {
            return dataIds.isEmpty();
        }
    }

    static Evidence confidentialOrAboveHeldIn(AuthorizationState state) {
        Set<DataAsset> confidentialOrAbove = state.heldAssets().stream()
            .filter(asset -> asset.declaredClassification().atLeast(Classification.CONFIDENTIAL))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

        if (confidentialOrAbove.isEmpty()) {
            return new Evidence(Set.of(), List.of());
        }

        Set<String> dataIds = confidentialOrAbove.stream()
            .map(DataAsset::id)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

        List<String> actionIds = state.trajectory().actions().stream()
            .filter(record -> record.decision() == Decision.ALLOW
                && record.action().type() == ActionType.READ
                && dataIds.contains(record.action().resource()))
            .map(ActionRecord::id)
            .toList();

        return new Evidence(dataIds, actionIds);
    }
}
