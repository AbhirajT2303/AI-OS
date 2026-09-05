package com.aios.authz.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyEvaluationTest {

    @Test
    void rejectsBlankReasonRegardlessOfDecision() {
        for (Decision decision : Decision.values()) {
            assertThatThrownBy(() ->
                new PolicyEvaluation("policy-1", decision, "  ", Set.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void aDenyNeedsNoEvidenceWhenTheReasonAloneExplainsIt() {
        // An RBAC denial has nothing meaningful to cite as "contributing data" or
        // "contributing prior actions" — the missing grant itself is the reason.
        // Evidence sets are for lineage-driven denials (ProvenanceBoundaryPolicy,
        // ENG-25), not a universal requirement on every DENY.
        PolicyEvaluation rbacDeny = new PolicyEvaluation(
            "rbac-precheck", Decision.DENY,
            "agent-42 has no grant for SEND_EXTERNAL on report-123", Set.of(), List.of());

        assertThat(rbacDeny.contributingDataIds()).isEmpty();
        assertThat(rbacDeny.contributingActionIds()).isEmpty();
    }

    @Test
    void nullEvidenceCollectionsDefaultToEmptyNotNull() {
        PolicyEvaluation evaluation = new PolicyEvaluation(
            "policy-1", Decision.ALLOW, "no objection", null, null);

        assertThat(evaluation.contributingDataIds()).isNotNull().isEmpty();
        assertThat(evaluation.contributingActionIds()).isNotNull().isEmpty();
    }
}
