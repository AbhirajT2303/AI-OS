# ADR-0004 — In-memory state store first, PostgreSQL when it earns its place

**Status:** Accepted
**Date:** 2026-09-05
**Relates to:** [PROJECT_CONTEXT.md](../../PROJECT_CONTEXT.md) §11, §41.15, §41.16

## Context

The recommended stack names PostgreSQL. The research question, however, is about
authorization semantics — nothing in Scenarios A–G requires durability.

## Decision

Ship `AuthorizationStateStore` as an interface with an in-memory implementation for Sprints
1–3. Add a JPA/PostgreSQL implementation in Sprint 4, when multi-agent workflows and
performance measurement make persistence genuinely useful.

## Consequences

**Positive**

- Sprint 1 delivers a running engine with no schema, no migrations, no container.
- Tests stay fast, which keeps the scenario suite runnable on every change.
- Writing to the interface from day one means the swap is additive, not a rewrite.

**Negative**

- Provenance-graph performance under load (kill condition D) is unmeasurable until the real
  store lands. Mitigated by making performance measurement an explicit Sprint 4 story
  (ENG-24) rather than an afterthought.
- Risk of the in-memory implementation quietly assuming single-JVM semantics. Mitigated by
  keeping the interface free of anything a networked store could not honour — no iteration
  over all workflows, no cross-workflow queries.

**Trigger for the switch:** whichever comes first — multi-agent delegation needing shared
state (Sprint 4), or a measurement that needs a realistic graph size. Not "because the stack
document says PostgreSQL."
