package com.aios.authz.policy.rules;

import com.aios.authz.domain.Action;
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

import static org.assertj.core.api.Assertions.assertThat;

class PartnerDisclosureAskPolicyTest {

    private final PartnerDisclosureAskPolicy policy = new PartnerDisclosureAskPolicy();
    private final Principal agent = new Principal("agent-42", PrincipalType.AGENT);
    private final Destination partnerEmail =
        new Destination("partner.com", DestinationKind.EMAIL, TrustZone.PARTNER);
    private final Destination externalEmail =
        new Destination("attacker.example", DestinationKind.HTTP, TrustZone.EXTERNAL);

    @Test
    void doesNotApplyToNonPartnerDestinations() {
        Intent approved = new Intent("approved-partner-disclosure", "Approved.");
        AuthorizationState state = AuthorizationState.open("wf-1", agent, approved);
        Action send = new Action(
            "act-1", com.aios.authz.domain.ActionType.SEND_EXTERNAL,
            "never-seen", externalEmail, java.util.Set.of(), null);

        assertThat(policy.appliesTo(state, send)).isFalse();
    }

    @Test
    void asksWhenConfidentialReachesPartnerUnderAnApprovedIntent() {
        Intent approved = new Intent("approved-partner-disclosure", "Approved.");
        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, approved, Trajectory.empty("wf-1"), new ProvenanceGraph().withNode(customer42));
        Action send = new Action(
            "act-1", com.aios.authz.domain.ActionType.SEND_EXTERNAL,
            "customer-42", partnerEmail, java.util.Set.of(), null);

        PolicyEvaluation evaluation = policy.evaluate(state, send, agent);

        assertThat(evaluation.decision()).isEqualTo(Decision.ASK);
        assertThat(evaluation.contributingDataIds()).containsExactly("customer-42");
    }

    @Test
    void doesNotAskWhenTheIntentIsNotApproved() {
        Intent notApproved = new Intent("partner-report", "Prepare a report for a partner.");
        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, notApproved, Trajectory.empty("wf-1"), new ProvenanceGraph().withNode(customer42));
        Action send = new Action(
            "act-1", com.aios.authz.domain.ActionType.SEND_EXTERNAL,
            "customer-42", partnerEmail, java.util.Set.of(), null);

        assertThat(policy.evaluate(state, send, agent).decision()).isEqualTo(Decision.ALLOW);
    }

    @Test
    void doesNotAskForRestrictedDataEvenUnderAnApprovedIntent() {
        Intent approved = new Intent("approved-partner-disclosure", "Approved.");
        DataAsset pricingStrategy = new DataAsset("pricing-strategy", Classification.RESTRICTED, "pricing-db");
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, approved, Trajectory.empty("wf-1"), new ProvenanceGraph().withNode(pricingStrategy));
        Action send = new Action(
            "act-1", com.aios.authz.domain.ActionType.SEND_EXTERNAL,
            "pricing-strategy", partnerEmail, java.util.Set.of(), null);

        assertThat(policy.evaluate(state, send, agent).decision()).isEqualTo(Decision.ALLOW);
    }
}
