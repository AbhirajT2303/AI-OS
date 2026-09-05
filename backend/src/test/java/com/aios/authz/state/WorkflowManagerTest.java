package com.aios.authz.state;

import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.PrincipalType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
