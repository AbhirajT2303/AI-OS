# ADR-0002 — Implement policy semantics directly, not on OPA or Cedar

**Status:** Accepted
**Date:** 2026-09-05
**Relates to:** [PROJECT_CONTEXT.md](../../PROJECT_CONTEXT.md) §11, §41.7

## Context

OPA/Rego and Cedar are mature policy engines. AWS Bedrock AgentCore already uses Cedar. The
obvious move is to build on one.

But the research question is whether *the authorization model itself* needs to change. If we
express our policies in Cedar and they work, we learn that Cedar is expressive enough — which
would weaken our own thesis by a route we did not intend to take. If they do not work, we
will not know whether the limit is Cedar's or ours.

## Decision

Implement policy evaluation directly in Java for 1G. Keep the `Policy` interface narrow
enough that a Cedar- or OPA-backed implementation can be dropped in behind it later.

## Consequences

**Positive**

- The trajectory-state-transition semantics are stated in the clearest possible terms with
  no framework mediating them.
- No time lost learning a policy DSL that may not survive the phase.
- 1H can compare *our* semantics against Cedar/Rego on equal footing — the honest version of
  the competitive comparison in [PROJECT_CONTEXT.md](../../PROJECT_CONTEXT.md) §32.

**Negative**

- We rebuild things these engines provide (evaluation, combination, testing tooling).
- Risk of accidentally inventing a worse policy language. Mitigated by keeping policies as
  plain Java classes with no DSL of their own — if we find ourselves designing syntax, that
  is the signal to adopt Cedar.

**Follow-up (1H, story ATK-11):** attempt to express Scenario E and Scenario F as Cedar
policies and as Rego. Whether they can be expressed *naturally* — without an external
correlation store feeding them accumulated trajectory — is close to the whole thesis. A
finding of "trivially expressible in Cedar" is kill condition A and must be reported as
such.
