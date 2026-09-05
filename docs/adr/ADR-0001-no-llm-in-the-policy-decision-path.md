# ADR-0001 — No LLM in the policy decision path

**Status:** Accepted
**Date:** 2026-09-05
**Relates to:** [PROJECT_CONTEXT.md](../../PROJECT_CONTEXT.md) §26 Principle 2, kill condition E

## Context

The system authorizes actions proposed by LLM-driven agents. It would be tempting to ask a
model "is this trajectory safe?" — it would handle nuance no rule set can, and it would demo
well.

## Decision

No LLM participates in producing an authorization decision. Policies are deterministic
functions of `(AuthorizationState, Action)`.

## Consequences

**Positive**

- Decisions are reproducible: same state + same action ⇒ same decision, always. This is a
  precondition for the 1G experiment being an experiment at all.
- The engine cannot be prompt-injected. Untrusted content reaches the model, never the
  policy evaluator.
- Decisions are explainable by construction, from the evidence a policy cites.
- Latency is measurable and bounded, which kill condition D depends on.

**Negative**

- Nuance must be encoded by hand. Cases like "is this aggregate sufficiently anonymised"
  cannot be expressed, and will surface as false positives.
- Policy authoring becomes an ongoing cost — which is itself a finding worth reporting.

**Explicitly deferred, not rejected:** an LLM may later *propose* candidate policies offline,
or *explain* a decision after it has been made deterministically. Neither is on the decision
path. Kill condition E states that if the final decision requires an LLM's subjective
judgment, the thesis is in serious trouble — so this ADR is also how that condition stays
falsifiable.
