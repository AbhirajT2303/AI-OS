package com.aios.authz.engine;

import com.aios.authz.domain.AuthorizationRequest;
import com.aios.authz.domain.AuthorizationResult;

/**
 * Something that can authorize a proposed action. Two implementations run
 * against identical input: {@link RbacBaselineEngine}, the control group, and
 * {@code TrajectoryAwareEngine} (ENG-21), the experiment. A trajectory-aware
 * decision is only meaningful measured against this baseline — see
 * docs/SCENARIOS.md.
 */
public interface AuthorizationEngine {

    String name();

    AuthorizationResult authorize(AuthorizationRequest request);
}
