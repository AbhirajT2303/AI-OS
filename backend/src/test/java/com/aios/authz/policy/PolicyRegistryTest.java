package com.aios.authz.policy;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.Destination;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.PolicyEvaluation;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.PrincipalType;
import com.aios.authz.state.AuthorizationState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyRegistryTest {

    private final AuthorizationState state = AuthorizationState.open(
        "wf-1", new Principal("agent-42", PrincipalType.AGENT),
        new Intent("partner-report", "Prepare a report for a partner."));
    private final Action read = new Action(
        "act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42");

    @Test
    void nonApplicablePoliciesAreOmittedNotVacuouslyAllowed() {
        Policy abstains = fixedPolicy("abstainer", false, Decision.ALLOW);
        PolicyRegistry registry = new PolicyRegistry(List.of(abstains));

        assertThat(registry.evaluate(state, read)).isEmpty();
    }

    @Test
    void everyApplicablePolicyContributesAnEvaluation() {
        Policy first = fixedPolicy("first", true, Decision.ALLOW);
        Policy second = fixedPolicy("second", true, Decision.DENY);
        PolicyRegistry registry = new PolicyRegistry(List.of(first, second));

        List<PolicyEvaluation> evaluations = registry.evaluate(state, read);

        assertThat(evaluations).extracting(PolicyEvaluation::policyId).containsExactly("first", "second");
    }

    private static Policy fixedPolicy(String id, boolean applies, Decision decision) {
        return new Policy() {
            @Override public String id() { return id; }
            @Override public boolean appliesTo(AuthorizationState s, Action a) { return applies; }
            @Override public PolicyEvaluation evaluate(AuthorizationState s, Action a) {
                return new PolicyEvaluation(id, decision, "fixed for test", Set.of(), List.of());
            }
        };
    }
}
