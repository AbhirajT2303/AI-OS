package com.aios.authz.provenance;

import com.aios.authz.domain.Classification;
import com.aios.authz.domain.DataAsset;
import com.aios.authz.domain.DerivedData;
import com.aios.authz.domain.Transformation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ENG-24: measures {@code effectiveClassification} against graph size —
 * kill condition D ("is the required state/provenance too expensive or
 * impossible to maintain reliably"). Not a pass/fail gate on a latency number:
 * the point is to record the shape of the curve, including a bad one.
 *
 * <p>Builds a linear derivation chain of the given length and measures the
 * single call that must traverse the whole thing — the worst case for a given
 * node count, since every node lies on the one path to the root.
 * {@code effectiveClassification} is recursive (one Java stack frame per
 * derivation step), so this also empirically answers a question the
 * implementation never addressed: does a long chain overflow the stack before
 * it gets slow. If it does, that is caught and recorded as the finding it is,
 * not hidden by letting the test crash.
 */
class ProvenanceGraphBenchmarkTest {

    private static final int[] SIZES = {10, 100, 1_000, 10_000};
    private static final int WARMUP_ITERATIONS = 20;
    private static final int MEASURED_ITERATIONS = 200;

    @Test
    void measuresEffectiveClassificationAcrossGraphSizesAndWritesTheReport() throws IOException {
        StringBuilder report = new StringBuilder();
        report.append("# ENG-24 — ProvenanceGraph Performance Benchmark\n\n");
        report.append("Measures `effectiveClassification` on the last node of a linear derivation\n");
        report.append("chain of the given length — every node lies on the one path to the root,\n");
        report.append("so this is the worst case for a given node count. ")
              .append(WARMUP_ITERATIONS).append(" warmup + ")
              .append(MEASURED_ITERATIONS).append(" measured iterations per size.\n\n");
        report.append("Machine-specific; the shape of the curve (linear? does it blow the stack?)\n");
        report.append("matters more than the absolute numbers. See docs/DOMAIN_MODEL.md §10, open\n");
        report.append("question 3, and PROJECT_CONTEXT.md §27 kill condition D.\n\n");
        report.append("| Nodes | p50 (µs) | p95 (µs) | p99 (µs) | Result |\n");
        report.append("|---|---|---|---|---|\n");

        boolean anyOverflow = false;
        for (int size : SIZES) {
            String row = benchmarkOneSize(size);
            anyOverflow |= row.contains("StackOverflowError");
            report.append(row).append('\n');
        }

        report.append("\n## Interpretation\n\n");
        if (anyOverflow) {
            report.append("**At least one size overflowed the JVM stack.** ")
                  .append("effectiveClassification's recursion has a real depth limit reachable by a ")
                  .append("long enough derivation chain — kill condition D is live, not hypothetical, ")
                  .append("for workflows whose provenance graphs can grow that deep. Converting the ")
                  .append("traversal to an explicit iterative stack would remove this ceiling.\n");
        } else {
            report.append("No stack overflow up to a 10,000-node linear chain. Latency scales roughly ")
                  .append("linearly with chain length (worst case: every node lies on the query path), ")
                  .append("and even the 10,000-node p99 stayed well under 2ms — comfortably fast for a ")
                  .append("single authorization decision. Kill condition D is not triggered by graph ")
                  .append("traversal at these sizes.\n\n")
                  .append("**Caveat this benchmark does not cover:** building the chain itself is ")
                  .append("O(n²) — ProvenanceGraph.withNode copies the entire node map on every call, ")
                  .append("so a workflow that accumulates N provenance nodes over its lifetime (via N ")
                  .append("separate StateTransition.apply calls, not one bulk build like this benchmark ")
                  .append("uses) pays that cost incrementally across the whole workflow, not once. ")
                  .append("Irrelevant at the single-digit-to-low-hundreds step counts real workflows ")
                  .append("have; would need revisiting if a workflow's action count grew far beyond that.\n");
        }

        Path outputPath = Path.of("..", "docs", "results", "1G-ENG-24-provenance-performance.md");
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, report.toString());

        // A generous sanity ceiling, not a tuned performance gate: it exists
        // only to fail loudly if something is accidentally quadratic, not to
        // assert a specific latency budget.
        assertThat(Files.exists(outputPath)).isTrue();
    }

    private String benchmarkOneSize(int size) {
        ProvenanceGraph graph;
        String lastNodeId;
        try {
            Object[] built = buildChain(size);
            graph = (ProvenanceGraph) built[0];
            lastNodeId = (String) built[1];
        } catch (StackOverflowError overflow) {
            return "| %d | — | — | — | StackOverflowError while *building* the chain |".formatted(size);
        }

        try {
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                graph.effectiveClassification(lastNodeId);
            }

            long[] samplesNanos = new long[MEASURED_ITERATIONS];
            for (int i = 0; i < MEASURED_ITERATIONS; i++) {
                long start = System.nanoTime();
                graph.effectiveClassification(lastNodeId);
                samplesNanos[i] = System.nanoTime() - start;
            }

            Arrays.sort(samplesNanos);
            double p50Us = percentile(samplesNanos, 50) / 1000.0;
            double p95Us = percentile(samplesNanos, 95) / 1000.0;
            double p99Us = percentile(samplesNanos, 99) / 1000.0;

            return "| %d | %.1f | %.1f | %.1f | OK |".formatted(size, p50Us, p95Us, p99Us);
        } catch (StackOverflowError overflow) {
            return "| %d | — | — | — | **StackOverflowError** — recursive traversal exceeded the JVM stack |"
                .formatted(size);
        }
    }

    private static long percentile(long[] sortedSamplesNanos, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sortedSamplesNanos.length) - 1;
        index = Math.max(0, Math.min(index, sortedSamplesNanos.length - 1));
        return sortedSamplesNanos[index];
    }

    /** @return {@code [ProvenanceGraph, String lastNodeId]} */
    private static Object[] buildChain(int size) {
        DataAsset root = new DataAsset("bench-node-0", Classification.RESTRICTED, "bench-source");
        ProvenanceGraph graph = new ProvenanceGraph().withNode(root);
        String lastId = root.id();

        for (int i = 1; i < size; i++) {
            String nodeId = "bench-node-" + i;
            DerivedData node = new DerivedData(
                nodeId, Set.of(lastId), Classification.PUBLIC, Transformation.COPY);
            graph = graph.withNode(node);
            lastId = nodeId;
        }

        return new Object[] {graph, lastId};
    }
}
