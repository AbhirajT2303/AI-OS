package com.aios.authz.engine;

import com.aios.authz.domain.Decision;
import com.aios.authz.domain.PolicyEvaluation;

import java.util.List;

/**
 * Builds the top-level explanation for a combined decision from the
 * evaluations that produced it. Never templated per scenario: it simply
 * surfaces the reason of whichever evaluation matches the combined decision —
 * the actual explanation quality comes from each {@code Policy} generating its
 * own reason from evidence (see {@code ProvenanceBoundaryPolicy}), not from
 * anything scenario-specific here.
 */
public final class ExplanationBuilder {

    private ExplanationBuilder() {
    }

    public static String build(Decision combined, List<PolicyEvaluation> evaluations) {
        return evaluations.stream()
            .filter(evaluation -> evaluation.decision() == combined)
            .map(PolicyEvaluation::reason)
            .findFirst()
            .orElse(combined.name());
    }
}
