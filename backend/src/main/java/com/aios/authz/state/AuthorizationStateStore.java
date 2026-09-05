package com.aios.authz.state;

import java.util.Optional;

/**
 * Loads and persists one workflow's {@link AuthorizationState} by id.
 *
 * <p>Deliberately has no operation a networked store couldn't honour (no
 * "list all workflows", no cross-workflow query) — see ADR-0004. The in-memory
 * implementation is the only one until ENG-30 (Sprint 4) adds a PostgreSQL
 * backend behind this same interface.
 */
public interface AuthorizationStateStore {

    Optional<AuthorizationState> find(String workflowId);

    void save(AuthorizationState state);
}
