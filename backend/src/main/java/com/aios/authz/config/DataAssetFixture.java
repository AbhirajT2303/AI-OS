package com.aios.authz.config;

import java.util.Set;

/**
 * Raw shape of one entry in {@code fixtures/data-assets.json}.
 *
 * <p>{@code classification} is validated against a local string set rather than
 * the real {@code Classification} enum, which does not exist yet (ENG-04, Sprint
 * 1). This is a deliberate stopgap: fixture loading needs to fail loudly on a bad
 * value now, without pulling forward the lattice logic that enum will carry.
 */
public record DataAssetFixture(String id, String classification, String source) {

    private static final Set<String> VALID_CLASSIFICATIONS =
        Set.of("PUBLIC", "INTERNAL", "CONFIDENTIAL", "RESTRICTED");

    public DataAssetFixture {
        FixtureValidation.requireNonBlank(id, "data asset", "id");
        FixtureValidation.requireNonBlank(source, "data asset '" + id + "'", "source");
        FixtureValidation.requireOneOf(
            classification, VALID_CLASSIFICATIONS, "data asset '" + id + "'", "classification");
    }
}
