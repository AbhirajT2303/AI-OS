package com.aios.authz.policy.rules;

import com.aios.authz.domain.Action;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ModelBoundaryPolicyTest {

    private final ModelBoundaryPolicy policy = new ModelBoundaryPolicy();
    private final Principal agent = new Principal("agent-42", PrincipalType.AGENT);
    private final Intent intent = new Intent("partner-report", "Prepare a report for a partner.");
    private final DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");

    private final Destination internalLlm =
        new Destination("internal-llm", DestinationKind.MODEL, TrustZone.INTERNAL);
    private final Destination externalLlm =
        new Destination("openai-api", DestinationKind.MODEL, TrustZone.EXTERNAL);

    @Test
    void doesNotApplyToActionsOtherThanCallModel() {
        Action read = new Action("act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), null);
        AuthorizationState state = AuthorizationState.open("wf-1", agent, intent);

        assertThat(policy.appliesTo(state, read)).isFalse();
    }

    @Test
    void scenarioB_confidentialDataToExternalModel_denies() {
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, intent, Trajectory.empty("wf-1"), new ProvenanceGraph().withNode(customer42));
        Action callExternal = new Action(
            "act-2", ActionType.CALL_MODEL, "customer-42", externalLlm, Set.of("customer-42"), "summary-1");

        assertThat(policy.evaluate(state, callExternal, agent).decision()).isEqualTo(Decision.DENY);
    }

    @Test
    void scenarioC_confidentialDataToInternalModel_allows() {
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, intent, Trajectory.empty("wf-1"), new ProvenanceGraph().withNode(customer42));
        Action callInternal = new Action(
            "act-2", ActionType.CALL_MODEL, "customer-42", internalLlm, Set.of("customer-42"), "summary-1");

        assertThat(policy.evaluate(state, callInternal, agent).decision()).isEqualTo(Decision.ALLOW);
    }

    @Test
    void scenarioBAndCDifferOnlyByDestinationTrustZone() {
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, intent, Trajectory.empty("wf-1"), new ProvenanceGraph().withNode(customer42));

        Action callInternal = new Action(
            "act-2", ActionType.CALL_MODEL, "customer-42", internalLlm, Set.of("customer-42"), "summary-1");
        Action callExternal = new Action(
            "act-3", ActionType.CALL_MODEL, "customer-42", externalLlm, Set.of("customer-42"), "summary-1");

        Decision internalDecision = policy.evaluate(state, callInternal, agent).decision();
        Decision externalDecision = policy.evaluate(state, callExternal, agent).decision();

        assertThat(callInternal.type()).isEqualTo(callExternal.type());
        assertThat(callInternal.resource()).isEqualTo(callExternal.resource());
        assertThat(internalDecision).isNotEqualTo(externalDecision);
    }

    @Test
    void scenarioA_publicDataToExternalModel_stillAllows() {
        DataAsset publicBrochure = new DataAsset("public-brochure", Classification.PUBLIC, "marketing-cms");
        AuthorizationState state = new AuthorizationState(
            "wf-1", agent, intent, Trajectory.empty("wf-1"), new ProvenanceGraph().withNode(publicBrochure));
        Action callExternal = new Action(
            "act-2", ActionType.CALL_MODEL, "public-brochure", externalLlm, Set.of("public-brochure"), "draft-1");

        PolicyEvaluation evaluation = policy.evaluate(state, callExternal, agent);

        assertThat(evaluation.decision()).isEqualTo(Decision.ALLOW);
    }
}
