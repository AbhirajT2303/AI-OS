package com.aios.authz.policy.rules;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionRecord;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.Destination;
import com.aios.authz.domain.DestinationKind;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.PolicyEvaluation;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.PrincipalType;
import com.aios.authz.domain.Trajectory;
import com.aios.authz.domain.TrustZone;
import com.aios.authz.state.AuthorizationState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RiskBudgetPolicyTest {

    private final RiskBudgetPolicy policy = new RiskBudgetPolicy();
    private final Principal agent = new Principal("agent-42", PrincipalType.AGENT);
    private final Intent intent = new Intent("partner-report", "Prepare a report for a partner.");
    private final Destination externalEmail =
        new Destination("partner.com", DestinationKind.EMAIL, TrustZone.EXTERNAL);

    @Test
    void alwaysApplies() {
        AuthorizationState state = AuthorizationState.open("wf-1", agent, intent);
        Action read = new Action("act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), null);

        assertThat(policy.appliesTo(state, read)).isTrue();
    }

    @Test
    void allowsAFreshWorkflowWithFullBudget() {
        AuthorizationState state = AuthorizationState.open("wf-1", agent, intent);
        Action read = new Action("act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), null);

        PolicyEvaluation evaluation = policy.evaluate(state, read, agent);

        assertThat(evaluation.decision()).isEqualTo(Decision.ALLOW);
        assertThat(evaluation.reason()).contains("100");
    }

    @Test
    void asksRatherThanDeniesOnceTheBudgetIsExhausted() {
        // SEND_EXTERNAL costs 5; twenty of them exactly exhaust a 100-point budget.
        Trajectory trajectory = Trajectory.empty("wf-1");
        for (int i = 0; i < 20; i++) {
            Action send = new Action(
                "act-" + i, ActionType.SEND_EXTERNAL, "resource-" + i, externalEmail, Set.of(), null);
            trajectory = trajectory.append(
                new ActionRecord("act-" + i, agent, send, Decision.ALLOW, Instant.now()));
        }
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, intent, trajectory, new com.aios.authz.provenance.ProvenanceGraph());

        Action oneMoreRead = new Action(
            "act-20", ActionType.READ, "customer-42", Destination.NONE, Set.of(), null);

        PolicyEvaluation evaluation = policy.evaluate(state, oneMoreRead, agent);

        assertThat(evaluation.decision()).isEqualTo(Decision.ASK);
        assertThat(evaluation.reason()).contains("0 of 100");
    }

    @Test
    void deniedActionsDoNotCountAgainstTheBudget() {
        Action deniedSend = new Action(
            "act-1", ActionType.SEND_EXTERNAL, "customer-42", externalEmail, Set.of(), null);
        Trajectory trajectory = Trajectory.empty("wf-1")
            .append(new ActionRecord("act-1", agent, deniedSend, Decision.DENY, Instant.now()));
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, intent, trajectory, new com.aios.authz.provenance.ProvenanceGraph());

        Action read = new Action("act-2", ActionType.READ, "customer-42", Destination.NONE, Set.of(), null);

        PolicyEvaluation evaluation = policy.evaluate(state, read, agent);

        assertThat(evaluation.decision()).isEqualTo(Decision.ALLOW);
        assertThat(evaluation.reason()).contains("99 of 100");
    }
}
