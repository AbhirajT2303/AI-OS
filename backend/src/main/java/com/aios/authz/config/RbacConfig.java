package com.aios.authz.config;

import com.aios.authz.domain.ActionType;
import com.aios.authz.engine.PermissionCatalog;
import com.aios.authz.engine.RbacBaselineEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Wires the RBAC baseline's {@link PermissionCatalog} from the raw fixture DTOs
 * {@link FixtureConfig} loads. This is the clean swap-in promised when those DTOs
 * were introduced (ENG-03): the fixture layer stays a dumb, validated JSON
 * shape, and this class is the only place that knows how to turn it into the
 * real domain-typed catalog.
 *
 * <p>{@code rbacBaselineEngine} is exposed under its concrete type, not
 * {@code AuthorizationEngine} — {@link EngineConfig} is what decides which
 * {@code AuthorizationEngine} is primary now that there are two
 * (ENG-21).
 */
@Configuration
public class RbacConfig {

    @Bean
    public PermissionCatalog permissionCatalog(@Autowired List<PermissionFixture> permissionFixtures) {
        List<PermissionCatalog.Grant> grants = new ArrayList<>();
        for (PermissionFixture fixture : permissionFixtures) {
            for (String actionTypeName : fixture.actionTypes()) {
                grants.add(new PermissionCatalog.Grant(
                    fixture.principalId(), ActionType.valueOf(actionTypeName), fixture.resourcePattern()));
            }
        }
        return new PermissionCatalog(grants);
    }

    @Bean
    public RbacBaselineEngine rbacBaselineEngine(PermissionCatalog permissionCatalog) {
        return new RbacBaselineEngine(permissionCatalog);
    }
}
