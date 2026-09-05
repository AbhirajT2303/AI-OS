package com.aios.authz.config;

import com.aios.authz.domain.ActionType;
import com.aios.authz.engine.PermissionCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RbacConfigTest {

    @Autowired
    private PermissionCatalog permissionCatalog;

    @Test
    void productionCatalogGrantsAgent42TheScenarioPermissions() {
        assertThat(permissionCatalog.permits("agent-42", ActionType.READ, "customer-42")).isTrue();
        assertThat(permissionCatalog.permits("agent-42", ActionType.SEND_EXTERNAL, "report-123")).isTrue();
    }

    @Test
    void productionCatalogGrantsScenarioFAgentsTheirPermissions() {
        assertThat(permissionCatalog.permits("agent-A", ActionType.READ, "customer-42")).isTrue();
        assertThat(permissionCatalog.permits("agent-A", ActionType.DELEGATE, "customer-42")).isTrue();
        assertThat(permissionCatalog.permits("agent-C", ActionType.SEND_EXTERNAL, "summary-2")).isTrue();
    }
}
