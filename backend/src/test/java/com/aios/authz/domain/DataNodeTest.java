package com.aios.authz.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataNodeTest {

    @Test
    void dataAssetDerivedFromIsEmptyNotNull() {
        DataAsset asset = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");

        assertThat(asset.derivedFrom()).isNotNull().isEmpty();
    }

    @Test
    void derivedDataRequiresAtLeastOneSource() {
        assertThatThrownBy(() -> new DerivedData(
            "report-123", Set.of(), Classification.PUBLIC, Transformation.GENERATE))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transformationDoesNotByItselfAlterDeclaredClassification() {
        // A DerivedData's declaredClassification is whatever the caller asserts for
        // it in isolation — Transformation is recorded for lineage only and never
        // changes it. The real, lineage-aware answer comes from
        // ProvenanceGraph.effectiveClassification (ENG-22), not from this type.
        // See docs/DOMAIN_MODEL.md §10, open question 1.
        DerivedData summary = new DerivedData(
            "summary-1", Set.of("customer-42"), Classification.PUBLIC, Transformation.SUMMARIZE);

        assertThat(summary.declaredClassification()).isEqualTo(Classification.PUBLIC);
    }

    @Test
    void sealedHierarchySwitchIsExhaustiveWithoutADefaultBranch() {
        DataNode asset = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        DataNode derived = new DerivedData(
            "summary-1", Set.of("customer-42"), Classification.PUBLIC, Transformation.SUMMARIZE);

        assertThat(describe(asset)).isEqualTo("asset:customer-42");
        assertThat(describe(derived)).isEqualTo("derived:summary-1");
    }

    // No default branch: this only compiles because the compiler proves the
    // switch is exhaustive over DataNode's sealed permits list (JEP 441).
    // Adding a third implementation of DataNode without updating this method
    // would fail to compile, which is the point.
    private static String describe(DataNode node) {
        return switch (node) {
            case DataAsset a -> "asset:" + a.id();
            case DerivedData d -> "derived:" + d.id();
        };
    }
}
