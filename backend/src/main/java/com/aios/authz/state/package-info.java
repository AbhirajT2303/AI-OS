/**
 * Authorization state for one workflow and its pure state-transition function.
 *
 * <p>Depends on {@link com.aios.authz.domain} and {@link com.aios.authz.provenance}.
 * {@code StateTransition.apply} must remain free of I/O and clock reads — see
 * docs/DOMAIN_MODEL.md §6.
 */
package com.aios.authz.state;
