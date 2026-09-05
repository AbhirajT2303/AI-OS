package com.aios.authz.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FixtureLoaderTest {

    private final FixtureLoader loader = new FixtureLoader();

    @Test
    void loadsProductionDataAssets() {
        List<DataAssetFixture> assets =
            loader.load("/fixtures/data-assets.json", DataAssetFixture.class);

        assertThat(assets).hasSize(5);
        assertThat(assets).extracting(DataAssetFixture::id)
            .contains("customer-42", "pricing-strategy", "support-tickets-42");
    }

    @Test
    void missingFixtureFailsLoudly() {
        assertThatThrownBy(() ->
            loader.load("/fixtures/does-not-exist.json", DataAssetFixture.class))
            .isInstanceOf(FixtureLoadException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void malformedClassificationFailsLoudlyRatherThanLoadingPartialData() {
        assertThatThrownBy(() ->
            loader.load("/fixtures/malformed-data-assets.json", DataAssetFixture.class))
            .isInstanceOf(FixtureLoadException.class);
    }

    @Test
    void overridesToAnAlternateLocationWithoutTouchingProductionConfig() {
        List<DataAssetFixture> testAssets =
            loader.load("/fixtures/test-only-data-assets.json", DataAssetFixture.class);

        assertThat(testAssets).hasSize(1);
        assertThat(testAssets.get(0).id()).isEqualTo("test-fixture-asset");
    }
}
