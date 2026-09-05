package com.aios.authz.config;

import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.engine.DataAssetCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Wires {@link DataAssetCatalog} from the raw fixture DTOs {@link FixtureConfig}
 * loads — the same clean swap-in pattern as {@link RbacConfig} for
 * {@code PermissionCatalog} (ENG-10).
 */
@Configuration
public class DataAssetConfig {

    @Bean
    public DataAssetCatalog dataAssetCatalog(List<DataAssetFixture> dataAssetFixtures) {
        List<DataAsset> assets = dataAssetFixtures.stream()
            .map(fixture -> new DataAsset(
                fixture.id(), Classification.valueOf(fixture.classification()), fixture.source()))
            .toList();
        return new DataAssetCatalog(assets);
    }
}
