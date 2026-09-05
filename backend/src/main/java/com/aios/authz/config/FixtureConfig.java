package com.aios.authz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Wires the production fixture set: the seed data assets, destinations and
 * permissions the scenario suite runs against (docs/SCENARIOS.md). Loading
 * happens eagerly as bean initialization, so a malformed fixture fails
 * application startup rather than surfacing later as a confusing runtime error.
 */
@Configuration
public class FixtureConfig {

    private final FixtureLoader loader = new FixtureLoader();

    @Bean
    public List<DataAssetFixture> dataAssetFixtures() {
        List<DataAssetFixture> assets =
            loader.load("/fixtures/data-assets.json", DataAssetFixture.class);
        requireUniqueIds(assets, DataAssetFixture::id, "data asset");
        return assets;
    }

    @Bean
    public List<DestinationFixture> destinationFixtures() {
        List<DestinationFixture> destinations =
            loader.load("/fixtures/destinations.json", DestinationFixture.class);
        requireUniqueIds(destinations, DestinationFixture::id, "destination");
        return destinations;
    }

    @Bean
    public List<PermissionFixture> permissionFixtures() {
        return loader.load("/fixtures/permissions.json", PermissionFixture.class);
    }

    private static <T> void requireUniqueIds(
            List<T> items, Function<T, String> idExtractor, String kind) {
        Set<String> seen = new HashSet<>();
        for (T item : items) {
            String id = idExtractor.apply(item);
            if (!seen.add(id)) {
                throw new FixtureLoadException("Duplicate " + kind + " id: " + id);
            }
        }
    }
}
