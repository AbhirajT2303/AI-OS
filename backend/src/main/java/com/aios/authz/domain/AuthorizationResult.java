package com.aios.authz.domain;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * The full outcome of authorizing one proposed action: the combined decision,
 * a generated explanation, and every policy evaluation that ran — including
 * abstentions — so the decision can be audited, not just trusted.
 */
public record AuthorizationResult(
        Decision decision,
        String explanation,
        List<PolicyEvaluation> evaluations,
        Duration evaluationTime) {

    public AuthorizationResult {
        Objects.requireNonNull(decision, "AuthorizationResult decision must not be null");
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("AuthorizationResult explanation must not be blank");
        }
        evaluations = evaluations == null ? List.of() : List.copyOf(evaluations);
        Objects.requireNonNull(evaluationTime, "AuthorizationResult evaluationTime must not be null");
    }
}
