package com.aios.authz.provenance;

import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.DerivedData;
import com.aios.authz.domain.Transformation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProvenanceGraphTest {

    @Test
    void unknownNodeThrows() {
        ProvenanceGraph graph = new ProvenanceGraph();

        assertThatThrownBy(() -> graph.effectiveClassification("nonexistent"))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void aSourceAssetsEffectiveClassificationIsItsOwnDeclaredOne() {
        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        ProvenanceGraph graph = new ProvenanceGraph().withNode(customer42);

        assertThat(graph.effectiveClassification("customer-42")).isEqualTo(Classification.CONFIDENTIAL);
    }

    @Test
    void effectiveClassificationIsTheLeastUpperBoundOverAllSources() {
        // The canonical scenario: report-123 <- draft-1 <- analytics-1 <- {customer-42 (CONFIDENTIAL),
        // pricing-strategy (RESTRICTED), support-tickets-42 (INTERNAL)}. Expected effective = RESTRICTED,
        // driven by pricing-strategy alone.
        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        DataAsset pricingStrategy = new DataAsset("pricing-strategy", Classification.RESTRICTED, "pricing-db");
        DataAsset supportTickets = new DataAsset("support-tickets-42", Classification.INTERNAL, "support-db");
        DerivedData analytics = new DerivedData(
            "analytics-1", Set.of("customer-42", "pricing-strategy", "support-tickets-42"),
            Classification.PUBLIC, Transformation.AGGREGATE);
        DerivedData draft = new DerivedData(
            "draft-1", Set.of("analytics-1"), Classification.PUBLIC, Transformation.GENERATE);
        DerivedData report = new DerivedData(
            "report-123", Set.of("draft-1"), Classification.PUBLIC, Transformation.GENERATE);

        ProvenanceGraph graph = new ProvenanceGraph()
            .withNode(customer42).withNode(pricingStrategy).withNode(supportTickets)
            .withNode(analytics).withNode(draft).withNode(report);

        assertThat(graph.effectiveClassification("report-123")).isEqualTo(Classification.RESTRICTED);
        assertThat(graph.rootsOf("report-123")).containsExactlyInAnyOrder(
            customer42, pricingStrategy, supportTickets);
        assertThat(graph.dominantRootsOf("report-123")).containsExactly(pricingStrategy);
    }

    @Test
    void isCycleSafe() {
        // a <- b <- a: a cycle that must not cause infinite recursion. Each
        // node's own declared classification still contributes once.
        DataAsset a = new DataAsset("a", Classification.CONFIDENTIAL, "source-a");
        DerivedData b = new DerivedData("b", Set.of("a"), Classification.PUBLIC, Transformation.COPY);
        // Overwrite "a" as if it were also derived from "b", forming a cycle a -> b -> a.
        DerivedData aAsCycle = new DerivedData("a", Set.of("b"), Classification.CONFIDENTIAL, Transformation.COPY);

        ProvenanceGraph graph = new ProvenanceGraph().withNode(a).withNode(b).withNode(aAsCycle);

        Classification result = graph.effectiveClassification("b");

        assertThat(result).isEqualTo(Classification.CONFIDENTIAL);
    }

    @Test
    void depthTenChainFromRestrictedStillResolvesRestricted() {
        ProvenanceGraph graph = new ProvenanceGraph();
        DataAsset root = new DataAsset("node-0", Classification.RESTRICTED, "source");
        graph = graph.withNode(root);

        for (int i = 1; i <= 10; i++) {
            DerivedData node = new DerivedData(
                "node-" + i, Set.of("node-" + (i - 1)), Classification.PUBLIC, Transformation.COPY);
            graph = graph.withNode(node);
        }

        assertThat(graph.effectiveClassification("node-10")).isEqualTo(Classification.RESTRICTED);
    }

    @Test
    void lineagePathReturnsTheDerivationChainRootFirst() {
        DataAsset pricingStrategy = new DataAsset("pricing-strategy", Classification.RESTRICTED, "pricing-db");
        DerivedData analytics = new DerivedData(
            "analytics-1", Set.of("pricing-strategy"), Classification.PUBLIC, Transformation.AGGREGATE);
        DerivedData report = new DerivedData(
            "report-123", Set.of("analytics-1"), Classification.PUBLIC, Transformation.GENERATE);

        ProvenanceGraph graph = new ProvenanceGraph()
            .withNode(pricingStrategy).withNode(analytics).withNode(report);

        assertThat(graph.lineagePath("pricing-strategy", "report-123"))
            .isEqualTo(List.of("pricing-strategy", "analytics-1", "report-123"));
    }

    @Test
    void lineagePathIsEmptyWhenNoPathExists() {
        DataAsset unrelated = new DataAsset("unrelated", Classification.PUBLIC, "source");
        ProvenanceGraph graph = new ProvenanceGraph().withNode(unrelated);

        assertThat(graph.lineagePath("unrelated", "does-not-exist")).isEmpty();
    }

    @Test
    void allRootsReturnsEveryDataAssetRegardlessOfWhatDerivesFromIt() {
        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        DerivedData summary = new DerivedData(
            "summary-1", Set.of("customer-42"), Classification.PUBLIC, Transformation.SUMMARIZE);

        ProvenanceGraph graph = new ProvenanceGraph().withNode(customer42).withNode(summary);

        assertThat(graph.allRoots()).containsExactly(customer42);
    }

    @Test
    void withNodeReturnsANewGraphRatherThanMutating() {
        ProvenanceGraph original = new ProvenanceGraph();
        DataAsset asset = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");

        ProvenanceGraph updated = original.withNode(asset);

        assertThat(original.find("customer-42")).isEmpty();
        assertThat(updated.find("customer-42")).contains(asset);
    }

    @Test
    void twoIndependentlyBuiltGraphsWithTheSameNodesAreEqual() {
        // Not object identity: this is what a round-trip through persistence
        // (ENG-30) produces — a brand new instance that must still compare
        // equal to the one that was saved.
        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        DataAsset pricingStrategy = new DataAsset("pricing-strategy", Classification.RESTRICTED, "pricing-db");

        ProvenanceGraph first = new ProvenanceGraph().withNode(customer42).withNode(pricingStrategy);
        ProvenanceGraph second = new ProvenanceGraph().withNode(pricingStrategy).withNode(customer42);

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void graphsWithDifferentNodesAreNotEqual() {
        DataAsset customer42 = new DataAsset("customer-42", Classification.CONFIDENTIAL, "customer-db");
        ProvenanceGraph withNode = new ProvenanceGraph().withNode(customer42);
        ProvenanceGraph empty = new ProvenanceGraph();

        assertThat(withNode).isNotEqualTo(empty);
    }
}
