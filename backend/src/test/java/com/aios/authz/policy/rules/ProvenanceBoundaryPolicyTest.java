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

class ProvenanceBoundaryPolicyTest {

    private final ProvenanceBoundaryPolicy policy = new ProvenanceBoundaryPolicy();
    private final Principal agent = new Principal("agent-42", PrincipalType.AGENT);
    private final Intent intent = new Intent("partner-report", "Prepare a report for a partner.");
    private final Destination partnerEmail =
        new Destination("partner.com", DestinationKind.EMAIL, TrustZone.PARTNER);
    private final Destination internalLlm =
        new Destination("internal-llm", DestinationKind.MODEL, TrustZone.INTERNAL);

    @Test
    void doesNotApplyWhenResourceIsUnknownToTheGraph() {
        AuthorizationState state = AuthorizationState.open("wf-1", agent, intent);
        Action send = new Action(
            "act-1", ActionType.SEND_EXTERNAL, "never-seen", partnerEmail, Set.of(), null);

        assertThat(policy.appliesTo(state, send)).isFalse();
    }

    @Test
    void allowsWhenEffectiveClassificationDoesNotExceedTheZoneMaximum() {
        DataAsset publicBrochure = new DataAsset("public-brochure", Classification.PUBLIC, "marketing-cms");
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, intent, Trajectory.empty("wf-1"), new ProvenanceGraph().withNode(publicBrochure));
        Action send = new Action(
            "act-2", ActionType.SEND_EXTERNAL, "public-brochure", partnerEmail, Set.of(), null);

        assertThat(policy.evaluate(state, send, agent).decision()).isEqualTo(Decision.ALLOW);
    }

    @Test
    void allowsWhenDestinationIsInternalRegardlessOfClassification() {
        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, intent, Trajectory.empty("wf-1"), new ProvenanceGraph().withNode(customer42));
        Action callInternal = new Action(
            "act-2", ActionType.CALL_MODEL, "customer-42", internalLlm, Set.of("customer-42"), "summary-1");

        assertThat(policy.evaluate(state, callInternal, agent).decision()).isEqualTo(Decision.ALLOW);
    }

    @Test
    void deniesAndCitesTheDominantContributorAndItsAcquiringAction() {
        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        DataAsset pricingStrategy = new DataAsset("pricing-strategy", Classification.RESTRICTED, "pricing-db");
        Action readCustomer = new Action(
            "act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42");
        Action readPricing = new Action(
            "act-2", ActionType.READ, "pricing-strategy", Destination.NONE, Set.of(), "pricing-strategy");
        Trajectory trajectory = Trajectory.empty("wf-1")
            .append(new ActionRecord("act-1", agent, readCustomer, Decision.ALLOW, Instant.now()))
            .append(new ActionRecord("act-2", agent, readPricing, Decision.ALLOW, Instant.now()));
        ProvenanceGraph graph = new ProvenanceGraph().withNode(customer42).withNode(pricingStrategy);
        AuthorizationState state = new AuthorizationState("wf-1", agent, intent, trajectory, graph);

        Action send = new Action(
            "act-3", ActionType.SEND_EXTERNAL, "pricing-strategy", partnerEmail, Set.of(), null);

        PolicyEvaluation evaluation = policy.evaluate(state, send, agent);

        assertThat(evaluation.decision()).isEqualTo(Decision.DENY);
        assertThat(evaluation.contributingDataIds()).containsExactly("pricing-strategy");
        assertThat(evaluation.contributingActionIds()).containsExactly("act-2");
        assertThat(evaluation.reason()).contains("RESTRICTED").contains("PARTNER");
    }

    /**
     * The precision gain over ConfidentialToExternalPolicy: that coarse policy
     * denies any SEND_EXTERNAL once *anything* confidential-or-above has ever
     * been read in the workflow, regardless of what is actually being sent.
     * Here, customer-42 (CONFIDENTIAL) is held, but the resource being sent —
     * public-summary — derives only from public-brochure. This must ALLOW.
     */
    @Test
    void allowsSendingUnrelatedPublicDataEvenWhileConfidentialDataIsSeparatelyHeld() {
        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        DataAsset publicBrochure = new DataAsset("public-brochure", Classification.PUBLIC, "marketing-cms");
        com.aios.authz.domain.DerivedData publicSummary = new com.aios.authz.domain.DerivedData(
            "public-summary", Set.of("public-brochure"), Classification.PUBLIC,
            com.aios.authz.domain.Transformation.SUMMARIZE);

        ProvenanceGraph graph = new ProvenanceGraph()
            .withNode(customer42).withNode(publicBrochure).withNode(publicSummary);
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, intent, Trajectory.empty("wf-1"), graph);

        Action send = new Action(
            "act-3", ActionType.SEND_EXTERNAL, "public-summary", partnerEmail, Set.of(), null);

        assertThat(policy.evaluate(state, send, agent).decision()).isEqualTo(Decision.ALLOW);
    }

    @Test
    void explanationNamesBothTheAcquiringPrincipalAndTheOneAttemptingToSend() {
        Principal agentA = new Principal("agent-A", PrincipalType.AGENT);
        Principal agentC = new Principal("agent-C", PrincipalType.AGENT);
        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        Action readCustomer = new Action(
            "act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42");
        Trajectory trajectory = Trajectory.empty("wf-1")
            .append(new ActionRecord("act-1", agentA, readCustomer, Decision.ALLOW, Instant.now()));
        ProvenanceGraph graph = new ProvenanceGraph().withNode(customer42);
        AuthorizationState state = new AuthorizationState("wf-1", agentA, intent, trajectory, graph);

        Action send = new Action(
            "act-2", ActionType.SEND_EXTERNAL, "customer-42", partnerEmail, Set.of(), null);

        // agent-C is evaluating a send it proposes itself — a different
        // principal from agent-A, who acquired the data three steps earlier.
        PolicyEvaluation evaluation = policy.evaluate(state, send, agentC);

        assertThat(evaluation.reason()).contains("agent-A").contains("agent-C");
    }

    @Test
    void stepsAsideForTheApprovedPartnerDisclosureCarveOutInsteadOfDenying() {
        Intent approved = new Intent("approved-partner-disclosure", "Approved.");
        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, approved, Trajectory.empty("wf-1"), new ProvenanceGraph().withNode(customer42));
        Action send = new Action(
            "act-1", ActionType.SEND_EXTERNAL, "customer-42", partnerEmail, Set.of(), null);

        // This policy alone must ALLOW here — PartnerDisclosureAskPolicy is what
        // turns this into an ASK; if this policy still denied, deny-overrides
        // would make the carve-out unreachable regardless of the other policy.
        assertThat(policy.evaluate(state, send, agent).decision()).isEqualTo(Decision.ALLOW);
    }

    @Test
    void restrictedDataStillDeniesEvenUnderAnApprovedPartnerDisclosureIntent() {
        Intent approved = new Intent("approved-partner-disclosure", "Approved.");
        DataAsset pricingStrategy = new DataAsset("pricing-strategy", Classification.RESTRICTED, "pricing-db");
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, approved, Trajectory.empty("wf-1"), new ProvenanceGraph().withNode(pricingStrategy));
        Action send = new Action(
            "act-1", ActionType.SEND_EXTERNAL, "pricing-strategy", partnerEmail, Set.of(), null);

        // The carve-out only ever narrows the CONFIDENTIAL+PARTNER cell — this
        // proves Scenario E's RESTRICTED denial isn't weakened by its existence.
        assertThat(policy.evaluate(state, send, agent).decision()).isEqualTo(Decision.DENY);
    }
}
