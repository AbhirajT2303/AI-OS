package com.aios.authz.state;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link ConcurrentHashMap}-backed store: individual {@code find}/{@code save}
 * calls are thread-safe, though a load-modify-save sequence across two calls is
 * not atomic — a race between two concurrent {@code authorize} calls on the same
 * workflow can still interleave. Acceptable for 1G; revisit if 1H's concurrency
 * attacks (ATK-02 and neighbours) show it matters.
 */
public final class InMemoryAuthorizationStateStore implements AuthorizationStateStore {

    private final Map<String, AuthorizationState> statesByWorkflowId = new ConcurrentHashMap<>();

    @Override
    public Optional<AuthorizationState> find(String workflowId) {
        return Optional.ofNullable(statesByWorkflowId.get(workflowId));
    }

    @Override
    public void save(AuthorizationState state) {
        Objects.requireNonNull(state, "state must not be null");
        statesByWorkflowId.put(state.workflowId(), state);
    }
}
