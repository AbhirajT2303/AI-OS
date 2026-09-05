package com.aios.authz.config;

import com.aios.authz.policy.Policy;
import com.aios.authz.policy.PolicyRegistry;
import com.aios.authz.policy.rules.ConfidentialToExternalPolicy;
import com.aios.authz.policy.rules.ModelBoundaryPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Registers every {@link Policy} bean and wires them into a
 * {@link PolicyRegistry}. Adding a policy to the running system means adding a
 * {@code @Bean} method here — never editing the registry or an engine.
 */
@Configuration
public class PolicyConfig {

    @Bean
    public Policy confidentialToExternalPolicy() {
        return new ConfidentialToExternalPolicy();
    }

    @Bean
    public Policy modelBoundaryPolicy() {
        return new ModelBoundaryPolicy();
    }

    @Bean
    public PolicyRegistry policyRegistry(List<Policy> policies) {
        return new PolicyRegistry(policies);
    }
}
