/**
 * The two engines under comparison: {@code RbacBaselineEngine}, the control
 * group, and {@code TrajectoryAwareEngine}, the experiment.
 *
 * <p>Depends on {@link com.aios.authz.domain}, {@link com.aios.authz.provenance},
 * {@link com.aios.authz.state} and {@link com.aios.authz.policy}. The baseline
 * engine must remain structurally unable to reach {@code state} or
 * {@code provenance} — see ENG-12 and docs/DOMAIN_MODEL.md §9.
 */
package com.aios.authz.engine;
