package com.aios.authz.config;

import java.util.Set;

/**
 * Raw shape of one entry in {@code fixtures/destinations.json}. {@code kind} and
 * {@code trustZone} are validated locally, ahead of the real {@code Destination}
 * and {@code TrustZone} types (ENG-06, Sprint 1) — see {@link DataAssetFixture}.
 */
public record DestinationFixture(String id, String kind, String trustZone) {

    private static final Set<String> VALID_KINDS =
        Set.of("MODEL", "TOOL", "DATASTORE", "EMAIL", "HTTP", "AGENT", "NONE");

    private static final Set<String> VALID_TRUST_ZONES =
        Set.of("INTERNAL", "PARTNER", "EXTERNAL");

    public DestinationFixture {
        FixtureValidation.requireNonBlank(id, "destination", "id");
        FixtureValidation.requireOneOf(kind, VALID_KINDS, "destination '" + id + "'", "kind");
        FixtureValidation.requireOneOf(
            trustZone, VALID_TRUST_ZONES, "destination '" + id + "'", "trustZone");
    }
}
