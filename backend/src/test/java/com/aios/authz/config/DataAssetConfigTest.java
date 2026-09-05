package com.aios.authz.config;

import com.aios.authz.domain.Classification;
import com.aios.authz.engine.DataAssetCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataAssetConfigTest {

    @Autowired
    private DataAssetCatalog dataAssetCatalog;

    @Test
    void productionCatalogResolvesTheScenarioDataAssets() {
        assertThat(dataAssetCatalog.find("customer-42"))
            .hasValueSatisfying(asset -> assertThat(asset.declaredClassification())
                .isEqualTo(Classification.CONFIDENTIAL));
        assertThat(dataAssetCatalog.find("pricing-strategy"))
            .hasValueSatisfying(asset -> assertThat(asset.declaredClassification())
                .isEqualTo(Classification.RESTRICTED));
    }
}
