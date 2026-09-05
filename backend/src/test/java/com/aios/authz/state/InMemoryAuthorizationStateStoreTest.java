package com.aios.authz.state;

import com.aios.authz.domain.Intent;
import com.aios.authz.domain.Principal;
import com.aios.authz.domain.PrincipalType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAuthorizationStateStoreTest {

    private final Principal agent = new Principal("agent-42", PrincipalType.AGENT);
    private final Intent intent = new Intent("partner-report", "Prepare a report for a partner.");

    @Test
    void findReturnsEmptyForAnUnknownWorkflow() {
        InMemoryAuthorizationStateStore store = new InMemoryAuthorizationStateStore();

        assertThat(store.find("wf-unknown")).isEmpty();
    }

    @Test
    void savedStateIsFoundByWorkflowId() {
        InMemoryAuthorizationStateStore store = new InMemoryAuthorizationStateStore();
        AuthorizationState state = AuthorizationState.open("wf-1", agent, intent);

        store.save(state);

        assertThat(store.find("wf-1")).contains(state);
    }

    @Test
    void concurrentFindAndSaveOnDifferentWorkflowsDoNotInterfere() throws InterruptedException {
        InMemoryAuthorizationStateStore store = new InMemoryAuthorizationStateStore();
        int workflowCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch done = new CountDownLatch(workflowCount);

        for (int i = 0; i < workflowCount; i++) {
            String workflowId = "wf-" + i;
            executor.submit(() -> {
                store.save(AuthorizationState.open(workflowId, agent, intent));
                done.countDown();
            });
        }

        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        for (int i = 0; i < workflowCount; i++) {
            assertThat(store.find("wf-" + i)).isPresent();
        }
    }
}
