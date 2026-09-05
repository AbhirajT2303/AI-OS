package com.aios.authz.config;

import com.aios.authz.engine.AuthorizationEngine;
import com.aios.authz.engine.DataAssetCatalog;
import com.aios.authz.engine.RbacBaselineEngine;
import com.aios.authz.engine.TrajectoryAwareEngine;
import com.aios.authz.policy.PolicyRegistry;
import com.aios.authz.state.AuthorizationStateStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Decides which {@link AuthorizationEngine} is primary. There are now two
 * candidates in the context — {@code RbacBaselineEngine} (its own bean, for the
 * harness comparison later) and this one — so {@code @Primary} resolves any
 * single-typed injection (the API controller) to {@link TrajectoryAwareEngine}
 * without ambiguity, while {@code List<AuthorizationEngine>} injection (the
 * harness, ENG-33) still sees both.
 */
@Configuration
public class EngineConfig {

    @Bean
    @Primary
    public AuthorizationEngine authorizationEngine(
            RbacBaselineEngine rbacBaselineEngine,
            AuthorizationStateStore stateStore,
            PolicyRegistry policyRegistry,
            DataAssetCatalog dataAssetCatalog) {
        return new TrajectoryAwareEngine(rbacBaselineEngine, stateStore, policyRegistry, dataAssetCatalog);
    }
}
