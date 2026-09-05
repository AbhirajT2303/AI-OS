package com.aios.authz.policy;

import com.aios.authz.domain.Decision;
import com.aios.authz.domain.PolicyEvaluation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionCombinerTest {

    @Test
    void emptyEvaluationsAllowsBecauseNothingObjected() {
        assertThat(DecisionCombiner.combine(List.of())).isEqualTo(Decision.ALLOW);
    }

    @Test
    void allAllowCombinesToAllow() {
        List<PolicyEvaluation> evaluations = List.of(
            evaluation("p1", Decision.ALLOW), evaluation("p2", Decision.ALLOW));

        assertThat(DecisionCombiner.combine(evaluations)).isEqualTo(Decision.ALLOW);
    }

    @Test
    void anyDenyWinsOverAllowAndAsk() {
        List<PolicyEvaluation> evaluations = List.of(
            evaluation("p1", Decision.ALLOW), evaluation("p2", Decision.ASK), evaluation("p3", Decision.DENY));

        assertThat(DecisionCombiner.combine(evaluations)).isEqualTo(Decision.DENY);
    }

    @Test
    void askWinsOverAllowWhenNoDenyIsPresent() {
        List<PolicyEvaluation> evaluations = List.of(
            evaluation("p1", Decision.ALLOW), evaluation("p2", Decision.ASK));

        assertThat(DecisionCombiner.combine(evaluations)).isEqualTo(Decision.ASK);
    }

    @Test
    void resultIsIndependentOfEvaluationOrderAcross200RandomPermutationsPerCase() {
        List<List<PolicyEvaluation>> cases = List.of(
            List.of(evaluation("p1", Decision.ALLOW), evaluation("p2", Decision.ALLOW)),
            List.of(evaluation("p1", Decision.ALLOW), evaluation("p2", Decision.ASK)),
            List.of(evaluation("p1", Decision.ALLOW), evaluation("p2", Decision.ASK), evaluation("p3", Decision.DENY)),
            List.of(evaluation("p1", Decision.DENY), evaluation("p2", Decision.DENY), evaluation("p3", Decision.ASK)));

        Random random = new Random(42);

        for (List<PolicyEvaluation> original : cases) {
            Decision expected = DecisionCombiner.combine(original);

            for (int i = 0; i < 200; i++) {
                List<PolicyEvaluation> shuffled = new ArrayList<>(original);
                java.util.Collections.shuffle(shuffled, random);

                assertThat(DecisionCombiner.combine(shuffled)).isEqualTo(expected);
            }
        }
    }

    private static PolicyEvaluation evaluation(String policyId, Decision decision) {
        return new PolicyEvaluation(policyId, decision, "fixed for test", Set.of(), List.of());
    }
}
