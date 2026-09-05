package com.aios.authz.engine;

import com.aios.authz.domain.ActionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The RBAC baseline's permission table: {@code (principalId, ActionType, resource)
 * → boolean}. An unknown principal has no grants and is therefore denied — there
 * is no implicit-allow default.
 *
 * <p>{@code resourcePattern} supports a trailing-{@code *} prefix wildcard
 * (e.g. {@code customer-*}); {@code *} alone is just the degenerate case with an
 * empty prefix, matching everything.
 */
public final class PermissionCatalog {

    private final Map<String, List<Grant>> grantsByPrincipal;

    public PermissionCatalog(List<Grant> grants) {
        Objects.requireNonNull(grants, "grants must not be null");
        Map<String, List<Grant>> byPrincipal = new HashMap<>();
        for (Grant grant : grants) {
            byPrincipal.computeIfAbsent(grant.principalId(), id -> new ArrayList<>()).add(grant);
        }
        Map<String, List<Grant>> frozen = new HashMap<>();
        byPrincipal.forEach((id, list) -> frozen.put(id, List.copyOf(list)));
        this.grantsByPrincipal = Map.copyOf(frozen);
    }

    public boolean permits(String principalId, ActionType actionType, String resource) {
        List<Grant> grants = grantsByPrincipal.get(principalId);
        if (grants == null) {
            return false;
        }
        return grants.stream().anyMatch(
            g -> g.actionType() == actionType && matches(g.resourcePattern(), resource));
    }

    private static boolean matches(String resourcePattern, String resource) {
        if (resourcePattern.endsWith("*")) {
            return resource.startsWith(resourcePattern.substring(0, resourcePattern.length() - 1));
        }
        return resourcePattern.equals(resource);
    }

    /** One grant: this principal may perform this action type on resources matching this pattern. */
    public record Grant(String principalId, ActionType actionType, String resourcePattern) {

        public Grant {
            if (principalId == null || principalId.isBlank()) {
                throw new IllegalArgumentException("Grant principalId must not be blank");
            }
            Objects.requireNonNull(actionType, "Grant actionType must not be null");
            if (resourcePattern == null || resourcePattern.isBlank()) {
                throw new IllegalArgumentException("Grant resourcePattern must not be blank");
            }
        }
    }
}
