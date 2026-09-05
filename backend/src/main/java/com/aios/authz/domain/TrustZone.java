package com.aios.authz.domain;

/**
 * The trust boundary a {@link Destination} sits behind. Generalises "external
 * model" / "external HTTP endpoint" / "external email" into one concept: they
 * are all the same kind of boundary crossing.
 */
public enum TrustZone {
    INTERNAL,
    PARTNER,
    EXTERNAL
}
