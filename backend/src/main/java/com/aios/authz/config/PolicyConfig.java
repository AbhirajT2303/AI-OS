package com.aios.authz.config;

import com.aios.authz.policy.Policy;
import com.aios.authz.policy.PolicyRegistry;
import com.aios.authz.policy.rules.PartnerDisclosureAskPolicy;
import com.aios.authz.policy.rules.ProvenanceBoundaryPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Registers every {@link Policy} bean and wires them into a
 * {@link PolicyRegistry}. Adding a policy to the running system means adding a
 * {@code @Bean} method here — never editing the registry or an engine.
 *
 * <p>{@code ConfidentialToExternalPolicy} and {@code ModelBoundaryPolicy}
 * (Sprint 2, ENG-19/ENG-20) are retired from this active set as of ENG-25:
 * {@link ProvenanceBoundaryPolicy} reproduces their correct decisions on
 * Scenarios A/B/C and additionally avoids a false positive they have — denying
 * a safe send of unrelated public data merely because something confidential
 * was read earlier in the same workflow. Their classes and tests remain in the
 * codebase; 1H's comparison work (ENG-33/34) still wants the coarse baseline
 * available standalone, just not layered into the production decision anymore.
 */
@Configuration
public class PolicyConfig {

    @Bean
    public Policy provenanceBoundaryPolicy() {
        return new ProvenanceBoundaryPolicy();
    }

    @Bean
    public Policy partnerDisclosureAskPolicy() {
        return new PartnerDisclosureAskPolicy();
    }

    @Bean
    public PolicyRegistry policyRegistry(List<Policy> policies) {
        return new PolicyRegistry(policies);
    }
}
