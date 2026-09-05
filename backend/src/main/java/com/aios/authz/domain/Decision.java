package com.aios.authz.domain;

/**
 * The outcome of one policy evaluation, or of combining several.
 *
 * <p>Precedence when combining is {@code DENY > ASK > ALLOW} (deny-overrides):
 * any DENY wins outright; otherwise any ASK wins; otherwise ALLOW. This is the
 * only safe default, and it is also what makes policy composition
 * order-independent — see {@code DecisionCombiner} (ENG-18).
 */
public enum Decision {
    ALLOW,
    DENY,
    ASK
}
