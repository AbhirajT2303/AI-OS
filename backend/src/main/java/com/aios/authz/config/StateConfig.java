package com.aios.authz.config;

import com.aios.authz.state.AuthorizationStateStore;
import com.aios.authz.state.InMemoryAuthorizationStateStore;
import com.aios.authz.state.WorkflowManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the in-memory {@link AuthorizationStateStore} (ADR-0004) and the
 * {@link WorkflowManager} that opens new workflows against it.
 */
@Configuration
public class StateConfig {

    @Bean
    public AuthorizationStateStore authorizationStateStore() {
        return new InMemoryAuthorizationStateStore();
    }

    @Bean
    public WorkflowManager workflowManager(AuthorizationStateStore stateStore) {
        return new WorkflowManager(stateStore);
    }
}
