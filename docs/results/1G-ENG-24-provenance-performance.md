# ENG-24 — ProvenanceGraph Performance Benchmark

Measures `effectiveClassification` on the last node of a linear derivation
chain of the given length — every node lies on the one path to the root,
so this is the worst case for a given node count. 20 warmup + 200 measured iterations per size.

Machine-specific; the shape of the curve (linear? does it blow the stack?)
matters more than the absolute numbers. See docs/DOMAIN_MODEL.md §10, open
question 3, and PROJECT_CONTEXT.md §27 kill condition D.

| Nodes | p50 (µs) | p95 (µs) | p99 (µs) | Result |
|---|---|---|---|---|
| 10 | 2.3 | 2.8 | 3.4 | OK |
| 100 | 7.3 | 11.9 | 18.9 | OK |
| 1000 | 81.5 | 93.9 | 104.5 | OK |
| 10000 | 451.8 | 588.1 | 875.5 | OK |

## Interpretation

No stack overflow up to a 10,000-node linear chain. Latency scales roughly linearly with chain length (worst case: every node lies on the query path), and even the 10,000-node p99 stayed well under 2ms — comfortably fast for a single authorization decision. Kill condition D is not triggered by graph traversal at these sizes.

**Caveat this benchmark does not cover:** building the chain itself is O(n²) — ProvenanceGraph.withNode copies the entire node map on every call, so a workflow that accumulates N provenance nodes over its lifetime (via N separate StateTransition.apply calls, not one bulk build like this benchmark uses) pays that cost incrementally across the whole workflow, not once. Irrelevant at the single-digit-to-low-hundreds step counts real workflows have; would need revisiting if a workflow's action count grew far beyond that.
