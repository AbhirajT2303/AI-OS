package com.aios.authz.engine;

import com.aios.authz.domain.Action;
import com.aios.authz.domain.ActionType;
import com.aios.authz.domain.AuthorizationRequest;
import com.aios.authz.domain.AuthorizationResult;
import com.aios.authz.domain.Decision;
import com.aios.authz.domain.PolicyEvaluation;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The control group: traditional {@code (Principal, Action, Resource) -> Decision}
 * authorization, using nothing but a permission table. Deliberately unable to
 * see trajectory or provenance — see ENG-12, which asserts this structurally —
 * so a trajectory-aware DENY on the same input is only meaningful because this
 * engine could not have produced it.
 *
 * <p>Never returns {@link Decision#ASK}: a permission either exists or it
 * doesn't.
 */
public final class RbacBaselineEngine implements AuthorizationEngine {

    private final PermissionCatalog catalog;

    public RbacBaselineEngine(PermissionCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
    }

    @Override
    public String name() {
        return "rbac-baseline";
    }

    @Override
    public AuthorizationResult authorize(AuthorizationRequest request) {
        long startNanos = System.nanoTime();

        String principalId = request.principal().id();
        Action action = request.requestedAction();
        ActionType type = action.type();
        String resource = action.resource();

        boolean permitted = catalog.permits(principalId, type, resource);
        Decision decision = permitted ? Decision.ALLOW : Decision.DENY;
        String reason = permitted
            ? "%s holds %s on %s".formatted(principalId, type, resource)
            : "%s has no grant for %s on %s".formatted(principalId, type, resource);

        PolicyEvaluation evaluation =
            new PolicyEvaluation("rbac-precheck", decision, reason, Set.of(), List.of());

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        return new AuthorizationResult(decision, reason, List.of(evaluation), elapsed);
    }
}
