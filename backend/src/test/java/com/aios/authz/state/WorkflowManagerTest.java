package com.aios.authz.state;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionRecord;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.Destination;
import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.PrincipalType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowManagerTest {

    private final Principal agent = new Principal("agent-42", PrincipalType.AGENT);
    private final Intent intent = new Intent("partner-report", "Prepare a report for a partner.");

    @Test
    void openPersistsAnEmptyInitialStateFindableByTheReturnedId() {
        InMemoryAuthorizationStateStore store = new InMemoryAuthorizationStateStore();
        WorkflowManager manager = new WorkflowManager(store);

        String workflowId = manager.open(agent, intent);

        assertThat(store.find(workflowId)).hasValueSatisfying(state -> {
            assertThat(state.initiator()).isEqualTo(agent);
            assertThat(state.intent()).isEqualTo(intent);
            assertThat(state.heldAssets()).isEmpty();
            assertThat(state.trajectory().actions()).isEmpty();
        });
    }

    @Test
    void eachOpenCallProducesADistinctWorkflowId() {
        WorkflowManager manager = new WorkflowManager(new InMemoryAuthorizationStateStore());

        String first = manager.open(agent, intent);
        String second = manager.open(agent, intent);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void findReturnsEmptyForAnUnknownWorkflow() {
        WorkflowManager manager = new WorkflowManager(new InMemoryAuthorizationStateStore());

        assertThat(manager.find("wf-never-opened")).isEmpty();
    }

    @Test
    void findReturnsTheStateOpenPersisted() {
        WorkflowManager manager = new WorkflowManager(new InMemoryAuthorizationStateStore());

        String workflowId = manager.open(agent, intent);

        assertThat(manager.find(workflowId)).isPresent();
    }

    @Test
    void delegateRejectsAnUnknownWorkflow() {
        WorkflowManager manager = new WorkflowManager(new InMemoryAuthorizationStateStore());

        assertThatThrownBy(() -> manager.delegate(
            "wf-never-opened", "agent-A", "agent-B", Set.of("customer-42")))
            .isInstanceOf(UnknownWorkflowException.class);
    }

    @Test
    void delegateRejectsDataTheWorkflowDoesNotHold() {
        InMemoryAuthorizationStateStore store = new InMemoryAuthorizationStateStore();
        WorkflowManager manager = new WorkflowManager(store);
        String workflowId = manager.open(agent, intent);

        assertThatThrownBy(() -> manager.delegate(
            workflowId, "agent-A", "agent-B", Set.of("customer-42")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("customer-42");
    }

    @Test
    void delegateRecordsATransferOfDataTheWorkflowActuallyHolds() {
        InMemoryAuthorizationStateStore store = new InMemoryAuthorizationStateStore();
        WorkflowManager manager = new WorkflowManager(store);
        String workflowId = manager.open(agent, intent);

        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        Action read = new Action(
            "act-1", ActionType.READ, "customer-42", Destination.NONE, Set.of(), "customer-42");
        ActionRecord record = new ActionRecord("act-1", agent, read, Decision.ALLOW, Instant.now());
        AuthorizationState afterRead = StateTransition.apply(manager.find(workflowId).orElseThrow(), record, customer42);
        store.save(afterRead);

        manager.delegate(workflowId, "agent-A", "agent-B", Set.of("customer-42"));

        AuthorizationState finalState = manager.find(workflowId).orElseThrow();
        assertThat(finalState.trajectory().delegations()).hasSize(1);
        assertThat(finalState.trajectory().delegations().get(0).transferredDataIds())
            .containsExactly("customer-42");
    }
}
