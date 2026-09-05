package com.aios.authz.domain;

import java.util.Objects;

/**
 * Where an {@link Action} sends or reaches. Carrying {@link TrustZone} here —
 * not just an id — is what makes an internal and an external {@code CALL_MODEL}
 * distinguishable, resolving the gap that made Scenarios B and C (see
 * docs/SCENARIOS.md) indistinguishable in the original sketch. See
 * docs/DOMAIN_MODEL.md §4 [GAP-1].
 */
public record Destination(String id, DestinationKind kind, TrustZone trustZone) {

    /** For actions with no external effect — reading, writing to no particular sink. */
    public static final Destination NONE =
        new Destination("none", DestinationKind.NONE, TrustZone.INTERNAL);

    public Destination {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Destination id must not be blank");
        }
        Objects.requireNonNull(kind, "Destination kind must not be null");
        Objects.requireNonNull(trustZone, "Destination trustZone must not be null");
    }
}
