package com.aios.authz.engine;

import com.aios.authz.domain.ActionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionCatalogTest {

    @Test
    void unknownPrincipalIsDenied() {
        PermissionCatalog catalog = new PermissionCatalog(List.of());

        assertThat(catalog.permits("nobody", ActionType.READ, "customer-42")).isFalse();
    }

    @Test
    void exactResourceMatchIsRequiredWithoutAWildcard() {
        PermissionCatalog catalog = new PermissionCatalog(List.of(
            new PermissionCatalog.Grant("agent-42", ActionType.READ, "customer-42")));

        assertThat(catalog.permits("agent-42", ActionType.READ, "customer-42")).isTrue();
        assertThat(catalog.permits("agent-42", ActionType.READ, "customer-43")).isFalse();
        assertThat(catalog.permits("agent-42", ActionType.WRITE, "customer-42")).isFalse();
    }

    @Test
    void trailingWildcardMatchesByPrefix() {
        PermissionCatalog catalog = new PermissionCatalog(List.of(
            new PermissionCatalog.Grant("agent-42", ActionType.READ, "customer-*")));

        assertThat(catalog.permits("agent-42", ActionType.READ, "customer-42")).isTrue();
        assertThat(catalog.permits("agent-42", ActionType.READ, "customer-99")).isTrue();
        assertThat(catalog.permits("agent-42", ActionType.READ, "pricing-strategy")).isFalse();
    }

    @Test
    void bareStarMatchesEveryResource() {
        PermissionCatalog catalog = new PermissionCatalog(List.of(
            new PermissionCatalog.Grant("agent-42", ActionType.SEND_EXTERNAL, "*")));

        assertThat(catalog.permits("agent-42", ActionType.SEND_EXTERNAL, "report-123")).isTrue();
        assertThat(catalog.permits("agent-42", ActionType.SEND_EXTERNAL, "anything-at-all")).isTrue();
    }
}
