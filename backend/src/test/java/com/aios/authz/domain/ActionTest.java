package com.aios.authz.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActionTest {

    @Test
    void rejectsNullDestinationRatherThanDefaultingSilently() {
        assertThatThrownBy(() ->
            new Action("act-1", ActionType.READ, "customer-42", null, Set.of(), null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullInputDataIdsDefaultsToEmptySetNotNull() {
        Action action = new Action(
            "act-1", ActionType.READ, "customer-42", Destination.NONE, null, null);

        assertThat(action.inputDataIds()).isNotNull().isEmpty();
    }

    @Test
    void outputDataIdIsNullableButNotBlank() {
        Action withOutput = new Action(
            "act-1", ActionType.WRITE, "report-123", Destination.NONE, Set.of(), "report-123");
        assertThat(withOutput.outputDataId()).isEqualTo("report-123");

        assertThatThrownBy(() ->
            new Action("act-1", ActionType.WRITE, "report-123", Destination.NONE, Set.of(), " "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void internalAndExternalCallModelDifferOnlyByDestinationTrustZone() {
        Destination internalLlm = new Destination("internal-llm", DestinationKind.MODEL, TrustZone.INTERNAL);
        Destination externalLlm = new Destination("openai-api", DestinationKind.MODEL, TrustZone.EXTERNAL);

        Action callInternal = new Action(
            "act-2", ActionType.CALL_MODEL, "customer-42", internalLlm, Set.of("customer-42"), "summary-1");
        Action callExternal = new Action(
            "act-3", ActionType.CALL_MODEL, "customer-42", externalLlm, Set.of("customer-42"), "summary-1");

        assertThat(callInternal.type()).isEqualTo(callExternal.type());
        assertThat(callInternal.destination().kind()).isEqualTo(callExternal.destination().kind());
        assertThat(callInternal.destination().trustZone())
            .isNotEqualTo(callExternal.destination().trustZone());
    }
}
