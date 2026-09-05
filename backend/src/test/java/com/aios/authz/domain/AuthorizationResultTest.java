package com.aios.authz.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationResultTest {

    @Test
    void rejectsBlankExplanation() {
        assertThatThrownBy(() ->
            new AuthorizationResult(Decision.ALLOW, " ", List.of(), Duration.ZERO))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void evaluationsListIsDefensivelyCopied() {
        List<PolicyEvaluation> mutable = new ArrayList<>();
        mutable.add(new PolicyEvaluation("policy-1", Decision.ALLOW, "no objection", Set.of(), List.of()));

        AuthorizationResult result =
            new AuthorizationResult(Decision.ALLOW, "allowed", mutable, Duration.ofMillis(3));
        mutable.clear();

        assertThat(result.evaluations()).hasSize(1);
    }

    @Test
    void carriesEveryEvaluationIncludingAbstentions() {
        PolicyEvaluation abstained =
            new PolicyEvaluation("model-boundary", Decision.ALLOW, "not applicable to READ", Set.of(), List.of());
        PolicyEvaluation denied =
            new PolicyEvaluation("provenance-boundary", Decision.DENY, "RESTRICTED to EXTERNAL",
                Set.of("pricing-strategy"), List.of("act-2"));

        AuthorizationResult result = new AuthorizationResult(
            Decision.DENY, "denied by provenance-boundary", List.of(abstained, denied), Duration.ofMillis(5));

        assertThat(result.evaluations()).containsExactly(abstained, denied);
    }
}
