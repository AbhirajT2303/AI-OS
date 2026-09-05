package com.aios.authz.domain;

import java.util.Objects;

/**
 * What an engine authorizes: a principal proposing an action within a workflow,
 * under a declared intent. Carries no trajectory — an engine that needs one
 * loads it server-side by {@code workflowId} rather than trusting a
 * client-supplied history, which would be trivially forgeable (the agent could
 * simply omit the read that makes its send unsafe). See docs/DOMAIN_MODEL.md §7.
 */
public record AuthorizationRequest(String workflowId, Principal principal, Intent intent, Action requestedAction) {

    public AuthorizationRequest {
        if (workflowId == null || workflowId.isBlank()) {
            throw new IllegalArgumentException("AuthorizationRequest workflowId must not be blank");
        }
        Objects.requireNonNull(principal, "AuthorizationRequest principal must not be null");
        Objects.requireNonNull(intent, "AuthorizationRequest intent must not be null");
        Objects.requireNonNull(requestedAction, "AuthorizationRequest requestedAction must not be null");
    }
}
