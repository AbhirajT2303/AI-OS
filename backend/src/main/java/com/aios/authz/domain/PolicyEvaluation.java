package com.aios.authz.domain;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The outcome of one policy evaluating one proposed action, with the evidence
 * behind it.
 *
 * <p>{@code reason} is required and non-blank for every decision, not only
 * DENY — this is the structural guarantee behind "every decision is
 * explainable" (docs/PROJECT_CONTEXT.md §41.9). {@code contributingDataIds} and
 * {@code contributingActionIds} carry additional evidence for
 * lineage-driven denials (e.g. {@code ProvenanceBoundaryPolicy}, ENG-25) but are
 * not required to be non-empty: a simple RBAC permission check has a
 * perfectly good reason with no prior data or action to cite.
 */
public record PolicyEvaluation(
        String policyId,
        Decision decision,
        String reason,
        Set<String> contributingDataIds,
        List<String> contributingActionIds) {

    public PolicyEvaluation {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("PolicyEvaluation policyId must not be blank");
        }
        Objects.requireNonNull(decision, "PolicyEvaluation decision must not be null");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                "PolicyEvaluation reason must not be blank — every decision must be explainable");
        }
        contributingDataIds = contributingDataIds == null ? Set.of() : Set.copyOf(contributingDataIds);
        contributingActionIds =
            contributingActionIds == null ? List.of() : List.copyOf(contributingActionIds);
    }
}
