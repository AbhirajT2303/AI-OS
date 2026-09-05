# ADR-0003 — Provenance as a lineage graph, not content inspection

**Status:** Accepted
**Date:** 2026-09-05
**Relates to:** [PROJECT_CONTEXT.md](../../PROJECT_CONTEXT.md) §19, §26 Principle 3

## Context

To decide whether a derived report may leave the enterprise there are two approaches:

1. **Content inspection** — scan the report for sensitive patterns. This is what DLP does.
2. **Lineage** — track that the report was derived from confidential sources, regardless of
   what it says.

## Decision

Track lineage. Classification of derived data is the least upper bound of the classifications
of its transitive sources. Raw content is never stored in, or consulted by, the authorization
layer.

## Consequences

**Positive**

- Catches the case content scanning structurally cannot: a report that leaks *conclusions*
  from confidential data — a competitive-pricing recommendation, say — while containing no
  sensitive string. Scenarios D, E and F all turn on this.
- Robust to paraphrase, translation, summarisation and encoding, none of which alter lineage.
- The authorization layer never holds sensitive content, so it is not itself a data-breach
  target. This matters for enterprise adoption.

**Negative**

- **Over-classification is the central risk.** Lineage never decays: a report derived from
  one confidential record stays confidential even after aggregation over a million rows. If
  this makes real workflows unusable, that is kill condition C, and we will have found it
  honestly.
- Requires every transformation to be observed. An agent that reads data outside the
  mediated path breaks lineage silently — the highest-value attack in 1H (ATK-01).
- The `Classification` total order cannot represent incomparable compartments (`PII` vs
  `TRADE_SECRET`). Accepted for 1G; revisit if it distorts results.

**Complementary, not competing:** lineage and content inspection catch different failures. A
real product would run both. We build only lineage because only lineage is the thesis.
