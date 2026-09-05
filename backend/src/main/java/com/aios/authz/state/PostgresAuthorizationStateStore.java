package com.aios.authz.state;

import tools.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.Optional;

/**
 * A {@link WorkflowStateJpaRepository}-backed {@link AuthorizationStateStore}:
 * serializes {@link AuthorizationStateSnapshot} to JSON in one column per
 * workflow. See {@link AuthorizationStateSnapshot} for why no polymorphic
 * (de)serialization is needed.
 *
 * <p>Not the default — {@code InMemoryAuthorizationStateStore} is (ADR-0004);
 * this exists so persistence can be swapped in without touching any caller of
 * {@link AuthorizationStateStore}.
 */
public final class PostgresAuthorizationStateStore implements AuthorizationStateStore {

    private final WorkflowStateJpaRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresAuthorizationStateStore(WorkflowStateJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Optional<AuthorizationState> find(String workflowId) {
        return repository.findById(workflowId)
            .map(entity -> objectMapper.readValue(entity.getStateJson(), AuthorizationStateSnapshot.class))
            .map(AuthorizationStateSnapshot::toAuthorizationState);
    }

    @Override
    public void save(AuthorizationState state) {
        Objects.requireNonNull(state, "state must not be null");
        String json = objectMapper.writeValueAsString(AuthorizationStateSnapshot.from(state));

        repository.findById(state.workflowId())
            .ifPresentOrElse(
                existing -> {
                    existing.setStateJson(json);
                    repository.save(existing);
                },
                () -> repository.save(new WorkflowStateEntity(state.workflowId(), json)));
    }
}
