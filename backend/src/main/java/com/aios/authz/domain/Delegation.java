package com.aios.authz.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * A recorded transfer of specific data from one principal to another within a
 * workflow. Naming {@code transferredDataIds} explicitly (rather than "one
 * principal handed off to another, somehow") is what lets an explanation later
 * say which principal is responsible for which data crossing an agent
 * boundary — see docs/DOMAIN_MODEL.md §5 and Scenario F.
 *
 * <p><strong>Bookkeeping, not a security boundary.</strong> Recording a
 * Delegation does not, by itself, do anything the authorization decision
 * depends on: {@code AuthorizationState}/{@code ProvenanceGraph} are already
 * scoped to the whole workflow, not to whichever principal is acting, so
 * Scenario F's DENY holds whether or not any Delegation was ever recorded. Two
 * principals sharing a {@code workflowId} directly achieves the same effect
 * without calling {@code POST /workflows/{id}/delegate} at all — this is
 * exactly ATK-04's "out-of-band agent-to-agent transfer bypassing DELEGATE",
 * and it is trivially possible today. Flagged here deliberately rather than
 * discovered only in 1H.
 */
public record Delegation(String fromPrincipalId, String toPrincipalId, Set<String> transferredDataIds, Instant timestamp) {

    public Delegation {
        if (fromPrincipalId == null || fromPrincipalId.isBlank()) {
            throw new IllegalArgumentException("Delegation fromPrincipalId must not be blank");
        }
        if (toPrincipalId == null || toPrincipalId.isBlank()) {
            throw new IllegalArgumentException("Delegation toPrincipalId must not be blank");
        }
        if (transferredDataIds == null || transferredDataIds.isEmpty()) {
            throw new IllegalArgumentException("Delegation must name at least one transferred data id");
        }
        transferredDataIds = Set.copyOf(transferredDataIds);
        Objects.requireNonNull(timestamp, "Delegation timestamp must not be null");
    }
}
