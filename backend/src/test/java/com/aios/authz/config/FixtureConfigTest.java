package com.aios.authz.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FixtureConfigTest {

    @Autowired
    private List<DataAssetFixture> dataAssetFixtures;

    @Autowired
    private List<DestinationFixture> destinationFixtures;

    @Autowired
    private List<PermissionFixture> permissionFixtures;

    @Test
    void productionFixturesLoadAtApplicationStartup() {
        assertThat(dataAssetFixtures).hasSize(5);
        assertThat(destinationFixtures).hasSize(5);
        assertThat(permissionFixtures).isNotEmpty();
    }

    @Test
    void everyScenarioDataAssetFromDocsIsPresent() {
        assertThat(dataAssetFixtures).extracting(DataAssetFixture::id).containsExactlyInAnyOrder(
            "public-brochure", "customer-42", "pricing-strategy",
            "support-tickets-42", "partner-nda-terms");
    }

    @Test
    void everyScenarioDestinationFromDocsIsPresent() {
        assertThat(destinationFixtures).extracting(DestinationFixture::id).containsExactlyInAnyOrder(
            "internal-llm", "openai-api", "partner.com", "attacker.example", "report-store");
    }
}
