package com.aios.authz.engine;

import com.aios.authz.domain.Decision;
import com.aios.authz.domain.PolicyEvaluation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ExplanationBuilderTest {

    @Test
    void picksTheReasonOfAnEvaluationMatchingTheCombinedDecision() {
        PolicyEvaluation allow = new PolicyEvaluation(
            "policy-a", Decision.ALLOW, "nothing objectionable", Set.of(), List.of());
        PolicyEvaluation deny = new PolicyEvaluation(
            "policy-b", Decision.DENY, "pricing-strategy exceeds PARTNER zone maximum PUBLIC", Set.of(), List.of());

        String explanation = ExplanationBuilder.build(Decision.DENY, List.of(allow, deny));

        assertThat(explanation).isEqualTo("pricing-strategy exceeds PARTNER zone maximum PUBLIC");
    }

    @Test
    void fallsBackToTheDecisionNameWhenNoEvaluationMatches() {
        String explanation = ExplanationBuilder.build(Decision.ALLOW, List.of());

        assertThat(explanation).isEqualTo("ALLOW");
    }
}
