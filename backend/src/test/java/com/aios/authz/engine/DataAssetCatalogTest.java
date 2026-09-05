package com.aios.authz.engine;

import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataAssetCatalogTest {

    @Test
    void findsAKnownAssetById() {
        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        DataAssetCatalog catalog = new DataAssetCatalog(List.of(customer42));

        assertThat(catalog.find("customer-42")).contains(customer42);
    }

    @Test
    void unknownResourceIsEmptyNotAnException() {
        DataAssetCatalog catalog = new DataAssetCatalog(List.of());

        assertThat(catalog.find("nonexistent")).isEmpty();
    }
}
