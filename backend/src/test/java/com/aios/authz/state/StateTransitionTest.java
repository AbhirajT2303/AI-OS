package com.aios.authz.state;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionRecord;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.Destination;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.DataNode;
import com.aios.authz.domain.DerivedData;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.PrincipalType;
import com.aios.authz.domain.TrustZone;
import com.aios.authz.domain.DestinationKind;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StateTransitionTest {

    private final Principal agent = new Principal("agent-42", PrincipalType.AGENT);
    private final Intent intent = new Intent("partner-report", "Prepare a report for a partner.");
    private final DataAsset customer42 =
        new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");

    @Test
    void allowedReadOfAResolvedAssetAddsItToHeldAssets() {
        AuthorizationState initial = AuthorizationState.open("wf-1", agent, intent);
        Action read = new Action(
            "act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42");
        ActionRecord record = new ActionRecord("act-1", agent, read, Decision.ALLOW, Instant.now());

        AuthorizationState next = StateTransition.apply(initial, record, customer42);

        assertThat(next.heldAssets()).containsExactly(customer42);
        assertThat(next.trajectory().actions()).containsExactly(record);
        assertThat(initial.heldAssets()).isEmpty();
    }

    @Test
    void deniedActionIsRecordedButDoesNotAddToHeldAssets() {
        AuthorizationState initial = AuthorizationState.open("wf-1", agent, intent);
        Action read = new Action(
            "act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42");
        ActionRecord record = new ActionRecord("act-1", agent, read, Decision.DENY, Instant.now());

        AuthorizationState next = StateTransition.apply(initial, record, customer42);

        assertThat(next.heldAssets()).isEmpty();
        assertThat(next.trajectory().actions()).containsExactly(record);
    }

    @Test
    void nonReadActionsDoNotAddToHeldAssetsEvenIfAResolvedAssetIsPassed() {
        AuthorizationState initial = AuthorizationState.open("wf-1", agent, intent);
        Action send = new Action(
            "act-1", ActionType.SEND_EXTERNAL, "customer-42", Destination.NONE, Set.of(), null);
        ActionRecord record = new ActionRecord("act-1", agent, send, Decision.ALLOW, Instant.now());

        AuthorizationState next = StateTransition.apply(initial, record, customer42);

        assertThat(next.heldAssets()).isEmpty();
    }

    @Test
    void unresolvedAssetIsSafelyIgnored() {
        AuthorizationState initial = AuthorizationState.open("wf-1", agent, intent);
        Action read = new Action(
            "act-1", ActionType.READ, "unknown-resource", Destination.NONE, Set.of(), null);
        ActionRecord record = new ActionRecord("act-1", agent, read, Decision.ALLOW, Instant.now());

        AuthorizationState next = StateTransition.apply(initial, record, null);

        assertThat(next.heldAssets()).isEmpty();
        assertThat(next.trajectory().actions()).containsExactly(record);
    }

    @Test
    void allowedActionWithOutputAndInputsRegistersADerivedDataNode() {
        AuthorizationState initial = withCustomer42Held();
        Destination internalLlm = new Destination("internal-llm", DestinationKind.MODEL, TrustZone.INTERNAL);
        Action callModel = new Action(
            "act-2", ActionType.CALL_MODEL, "customer-42", internalLlm, Set.of("customer-42"), "summary-1");
        ActionRecord record = new ActionRecord("act-2", agent, callModel, Decision.ALLOW, Instant.now());

        AuthorizationState next = StateTransition.apply(initial, record, null);

        DataNode summaryNode = next.provenanceGraph().find("summary-1").orElseThrow();
        assertThat(summaryNode).isInstanceOf(DerivedData.class);
        assertThat(summaryNode.derivedFrom()).containsExactly("customer-42");
        assertThat(next.provenanceGraph().effectiveClassification("summary-1"))
            .isEqualTo(Classification.CONFIDENTIAL);
    }

    @Test
    void deniedActionWithOutputDoesNotRegisterADerivedDataNode() {
        AuthorizationState initial = withCustomer42Held();
        Destination externalLlm = new Destination("openai-api", DestinationKind.MODEL, TrustZone.EXTERNAL);
        Action callModel = new Action(
            "act-2", ActionType.CALL_MODEL, "customer-42", externalLlm, Set.of("customer-42"), "summary-1");
        ActionRecord record = new ActionRecord("act-2", agent, callModel, Decision.DENY, Instant.now());

        AuthorizationState next = StateTransition.apply(initial, record, null);

        assertThat(next.provenanceGraph().find("summary-1")).isEmpty();
    }

    @Test
    void outputWithNoDeclaredInputsIsNotAddedRatherThanForcedIntoAMeaninglessDerivation() {
        AuthorizationState initial = AuthorizationState.open("wf-1", agent, intent);
        Destination internalLlm = new Destination("internal-llm", DestinationKind.MODEL, TrustZone.INTERNAL);
        Action callModel = new Action(
            "act-1", ActionType.CALL_MODEL, "some-resource", internalLlm, Set.of(), "orphan-output");
        ActionRecord record = new ActionRecord("act-1", agent, callModel, Decision.ALLOW, Instant.now());

        AuthorizationState next = StateTransition.apply(initial, record, null);

        assertThat(next.provenanceGraph().find("orphan-output")).isEmpty();
    }

    private AuthorizationState withCustomer42Held() {
        AuthorizationState initial = AuthorizationState.open("wf-1", agent, intent);
        Action read = new Action(
            "act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42");
        ActionRecord readRecord = new ActionRecord("act-1", agent, read, Decision.ALLOW, Instant.now());
        return StateTransition.apply(initial, readRecord, customer42);
    }
}
