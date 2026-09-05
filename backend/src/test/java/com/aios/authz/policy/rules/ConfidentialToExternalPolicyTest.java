package com.aios.authz.policy.rules;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionRecord;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.Destination;
import com.aios.authz.domain.DestinationKind;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.PolicyEvaluation;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.PrincipalType;
import com.aios.authz.domain.Trajectory;
import com.aios.authz.domain.TrustZone;
import com.aios.authz.provenance.ProvenanceGraph;
import com.aios.authz.state.AuthorizationState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConfidentialToExternalPolicyTest {

    private final ConfidentialToExternalPolicy policy = new ConfidentialToExternalPolicy();
    private final Principal agent = new Principal("agent-42", PrincipalType.AGENT);
    private final Intent intent = new Intent("partner-report", "Prepare a report for a partner.");
    private final Destination partnerEmail =
        new Destination("partner.com", DestinationKind.EMAIL, TrustZone.PARTNER);

    @Test
    void doesNotApplyToActionsOtherThanSendExternal() {
        Action read = new Action("act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), null);
        AuthorizationState state = AuthorizationState.open("wf-1", agent, intent);

        assertThat(policy.appliesTo(state, read)).isFalse();
    }

    @Test
    void allowsSendExternalWhenNoConfidentialDataIsHeld() {
        DataAsset publicBrochure = new DataAsset("public-brochure", Classification.PUBLIC, "marketing-cms");
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, intent, Trajectory.empty("wf-1"), new ProvenanceGraph().withNode(publicBrochure));
        Action send = new Action(
            "act-2", ActionType.SEND_EXTERNAL, "public-brochure", partnerEmail, Set.of(), null);

        PolicyEvaluation evaluation = policy.evaluate(state, send, agent);

        assertThat(evaluation.decision()).isEqualTo(Decision.ALLOW);
    }

    @Test
    void deniesSendExternalWhenConfidentialDataIsHeldAndCitesItsAcquiringAction() {
        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        Action read = new Action(
            "act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42");
        ActionRecord readRecord = new ActionRecord("act-1", agent, read, Decision.ALLOW, Instant.now());
        Trajectory trajectory = Trajectory.empty("wf-1").append(readRecord);
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, intent, trajectory, new ProvenanceGraph().withNode(customer42));

        Action send = new Action(
            "act-2", ActionType.SEND_EXTERNAL, "internal-report-1", partnerEmail, Set.of(), null);

        PolicyEvaluation evaluation = policy.evaluate(state, send, agent);

        assertThat(evaluation.decision()).isEqualTo(Decision.DENY);
        assertThat(evaluation.contributingDataIds()).containsExactly("customer-42");
        assertThat(evaluation.contributingActionIds()).containsExactly("act-1");
    }
}
