/**
 * The policy contract, the registry that discovers policies, and deny-overrides
 * combination of their evaluations. Concrete rules live in {@code policy.rules}.
 *
 * <p>Depends on {@link com.aios.authz.domain}, {@link com.aios.authz.provenance}
 * and {@link com.aios.authz.state}. Must not depend on {@code engine} or
 * {@code api} — see docs/ARCHITECTURE.md §3 and ADR-0002.
 */
package com.aios.authz.policy;
