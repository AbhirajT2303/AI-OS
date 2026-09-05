package com.aios.authz.config;

import java.util.List;
import java.util.Set;

/**
 * Raw shape of one entry in {@code fixtures/permissions.json}: the grant of a
 * set of action types over a resource pattern to a principal. Backs the RBAC
 * baseline's {@code PermissionCatalog} once it lands (ENG-10, Sprint 1).
 */
public record PermissionFixture(String principalId, List<String> actionTypes, String resourcePattern) {

    private static final Set<String> VALID_ACTION_TYPES = Set.of(
        "READ", "WRITE", "CALL_TOOL", "CALL_MODEL", "TRANSFORM",
        "SEND_EXTERNAL", "DELEGATE", "DELETE");

    public PermissionFixture {
        FixtureValidation.requireNonBlank(principalId, "permission grant", "principalId");
        FixtureValidation.requireNonBlank(
            resourcePattern, "permission grant for '" + principalId + "'", "resourcePattern");
        FixtureValidation.requireNonEmpty(
            actionTypes, "permission grant for '" + principalId + "'", "actionType");
        for (String actionType : actionTypes) {
            FixtureValidation.requireOneOf(
                actionType, VALID_ACTION_TYPES, "permission grant for '" + principalId + "'", "actionType");
        }
    }
}
