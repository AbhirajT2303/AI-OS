# Domain Model

**Status:** proposed for 1G. Supersedes the code sketches in [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md)
§13–20, which are illustrative rather than complete.

Five gaps in the original sketches block Scenarios B, C, E and F. This document resolves
them without changing the research hypothesis. Each resolution is marked **[GAP-n]**.

| Gap | Problem in the sketch | Resolution |
|---|---|---|
| GAP-1 | `CALL_MODEL` cannot distinguish an internal from an external model, so Scenario B (DENY) and Scenario C (ALLOW) are indistinguishable. | `Destination` with a `TrustZone`. |
| GAP-2 | `DerivedData` is defined but unrelated to `DataAsset`; `Trajectory` holds only `Set<DataAsset>`. Lineage cannot propagate. | `DataNode` sealed interface + `ProvenanceGraph`. |
| GAP-3 | `ASK` appears in the architecture and DoD but no code path produces it. | `Decision` is a three-valued enum, resolved by precedence. |
| GAP-4 | Scenario F (multi-agent) needs cross-agent lineage; state is per-principal with no delegation link. | State keyed by `workflowId`; explicit `Delegation`. |
| GAP-5 | "Every decision explainable" has no carrier type. | `PolicyEvaluation` + `AuthorizationResult`. |

---

## 1. Identity and purpose

```java
public record Principal(String id, PrincipalType type) {}

public enum PrincipalType { USER, AGENT, SERVICE }

public record Intent(String id, String description) {}
```

`Intent` is the *declared* objective of the workflow (e.g. `prepare_partner_report`). It is
declared once when the workflow opens and is immutable for its lifetime. It is an input to
policy, never inferred by the engine.

> **Threat note for 1H:** intent is self-declared by the caller and therefore attacker-
> influenceable via prompt injection. Policies must not treat it as trusted. Tracked as
> ATK-08 in [BACKLOG.md](../planning/BACKLOG.md).

---

## 2. Classification lattice

```java
public enum Classification {
    PUBLIC(0), INTERNAL(1), CONFIDENTIAL(2), RESTRICTED(3);

    private final int level;

    public boolean atLeast(Classification other) { return this.level >= other.level; }

    public static Classification max(Classification a, Classification b) {
        return a.level >= b.level ? a : b;
    }
}
```

A total order, deliberately. Real enterprises use lattices with incomparable compartments
(e.g. `PII` vs `TRADE_SECRET`); a total order is sufficient for 1G and is called out as a
known simplification in [ADR-0003](adr/ADR-0003-provenance-as-a-lineage-graph.md).

**Least upper bound is the core rule of the whole system:** derived information carries the
maximum classification of everything it was derived from.

---

## 3. Information and provenance **[GAP-2]**

Source data and derived data become one type so lineage can be walked uniformly.

```java
public sealed interface DataNode permits DataAsset, DerivedData {
    String id();
    Classification declaredClassification();
    Set<String> derivedFrom();          // empty for a source asset
}

public record DataAsset(
    String id,
    Classification declaredClassification,
    String source                        // e.g. "customer-db"
) implements DataNode {
    @Override public Set<String> derivedFrom() { return Set.of(); }
}

public record DerivedData(
    String id,
    Set<String> derivedFrom,
    Classification declaredClassification,   // usually PUBLIC; the graph computes the real one
    Transformation transformation
) implements DataNode {}

public enum Transformation {
    SUMMARIZE, AGGREGATE, TRANSLATE, CLASSIFY, EMBED, GENERATE, COPY
}
```

`Transformation` is recorded but does **not** downgrade classification in 1G. Whether any
transformation may declassify (e.g. `AGGREGATE` over a large enough population) is an open
research question — deliberately deferred, because assuming it would weaken the thesis in
our own favour.

### ProvenanceGraph

```java
public final class ProvenanceGraph {

    private final Map<String, DataNode> nodes;

    /** Least upper bound of declared classifications over the transitive closure of derivedFrom. */
    public Classification effectiveClassification(String nodeId);

    /** Every source DataAsset reachable from nodeId. Drives explanations. */
    public Set<DataAsset> rootsOf(String nodeId);

    /** Shortest derivation path from a root to nodeId. Drives explanations. */
    public List<String> lineagePath(String fromRootId, String toNodeId);
}
```

`effectiveClassification` is the primitive the thesis rests on. Cycle-safe (visited set) —
cycles are possible when an agent writes a derived artefact back into a source system.

Example — the canonical scenario:

```text
report-123                     effective = RESTRICTED
├── customer-42    CONFIDENTIAL
├── pricing-strategy RESTRICTED   ◀── the reason
└── support-tickets  INTERNAL
```

---

## 4. Actions and destinations **[GAP-1]**

```java
public record Action(
    String id,
    ActionType type,
    String resource,                 // what is read/written, or the input data node id
    Destination destination,         // never null; Destination.NONE for purely internal ops
    Set<String> inputDataIds,        // data nodes this action consumes
    String outputDataId              // data node this action produces, or null
) {}

public enum ActionType {
    READ, WRITE, CALL_TOOL, CALL_MODEL, TRANSFORM, SEND_EXTERNAL, DELEGATE, DELETE
}

public record Destination(String id, DestinationKind kind, TrustZone trustZone) {
    public static final Destination NONE =
        new Destination("none", DestinationKind.NONE, TrustZone.INTERNAL);
}

public enum DestinationKind { MODEL, TOOL, DATASTORE, EMAIL, HTTP, AGENT, NONE }

public enum TrustZone { INTERNAL, PARTNER, EXTERNAL }
```

`TrustZone` on the destination is what makes Scenarios B and C separable: both are
`CALL_MODEL`, but one targets `TrustZone.EXTERNAL` and the other `TrustZone.INTERNAL`. This
also generalises the boundary rule — an external *model*, an external *HTTP endpoint* and
an external *email* are the same kind of boundary crossing, which is the point.

`inputDataIds` / `outputDataId` are what let the engine extend the provenance graph as a
side effect of authorizing an action, rather than requiring the agent to declare lineage.

---

## 5. Trajectory and delegation **[GAP-4]**

```java
public record ActionRecord(
    String id,
    Principal actor,
    Action action,
    Decision decision,
    Instant timestamp
) {}

public record Delegation(
    String fromPrincipalId,
    String toPrincipalId,
    Set<String> transferredDataIds,
    Instant timestamp
) {}

public record Trajectory(
    String workflowId,
    List<ActionRecord> actions,
    List<Delegation> delegations
) {}
```

**The trajectory is keyed by `workflowId`, not by principal.** This single decision is what
makes Scenario F expressible: when Agent A reads confidential data and delegates to Agent B,
which delegates to Agent C, all three operate inside one workflow, so C's authorization
request carries A's accumulated provenance. C is denied for information it never directly
read.

Denied actions are recorded too. An agent retrying a denied action along a different path is
itself a signal, and 1H will attack exactly that.

---

## 6. Authorization state

```java
public record AuthorizationState(
    String workflowId,
    Principal initiator,
    Intent intent,
    Trajectory trajectory,
    ProvenanceGraph provenance,
    Set<String> heldDataIds,          // data currently available to the workflow
    Set<Capability> capabilities,
    RiskBudget riskBudget,
    Set<String> allowedDestinations
) {}

public record RiskBudget(int total, int spent) {
    public int remaining() { return total - spent; }
}
```

Every ALLOWed action produces a new state. State transition is a pure function:

```java
AuthorizationState next = StateTransition.apply(current, action);
```

Pure because determinism is testable, replayable, and attackable in 1H. No I/O, no clock
reads inside the transition — timestamps are passed in.

---

## 7. Requests, decisions and explanations **[GAP-3, GAP-5]**

```java
public record AuthorizationRequest(
    String workflowId,
    Principal principal,
    Intent intent,
    Action requestedAction
) {}
```

The client sends **no trajectory**. The engine loads it server-side from `workflowId`. A
client-supplied trajectory would be trivially forgeable — the agent could simply omit the
`READ` that makes its `SEND_EXTERNAL` unsafe. This differs from
[PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §15 and is a security fix, not a scope change.

```java
public enum Decision { ALLOW, DENY, ASK }

public record PolicyEvaluation(
    String policyId,
    Decision decision,
    String reason,                        // human-readable, generated not hardcoded
    Set<String> contributingDataIds,      // evidence: which data
    List<String> contributingActionIds    // evidence: which prior actions
) {}

public record AuthorizationResult(
    Decision decision,
    String explanation,
    List<PolicyEvaluation> evaluations,   // every policy, including those that abstained
    Duration evaluationTime
) {}
```

**Decision precedence:** `DENY > ASK > ALLOW`. Any policy returning DENY denies. Otherwise
any ASK asks. Otherwise allow. Deny-overrides is the only safe default; it is also what
makes policy composition order-independent, which matters for 1H.

`ASK` is returned when a policy identifies a boundary crossing that a human could
legitimately authorize — e.g. `CONFIDENTIAL` (not `RESTRICTED`) data to a `PARTNER` (not
`EXTERNAL`) destination under a matching declared intent. ASK is how we keep the false-
positive rate ([PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §23) from being the only lever.

---

## 8. Policy interface

```java
public interface Policy {
    String id();
    boolean appliesTo(AuthorizationState state, Action action);
    PolicyEvaluation evaluate(AuthorizationState state, Action action);
}
```

Policies are stateless, side-effect free, and independently testable. `appliesTo` keeps
`evaluations` readable by letting a policy abstain rather than return a vacuous ALLOW.

Adding a policy must never require editing the engine — registry-based discovery only.
That constraint is what will let policy logic be swapped for OPA/Cedar later
([ADR-0002](adr/ADR-0002-implement-policy-semantics-directly.md)).

---

## 9. Engines

```java
public interface AuthorizationEngine {
    String name();
    AuthorizationResult authorize(AuthorizationRequest request);
}
```

Two implementations, identical interface, identical input:

- **`RbacBaselineEngine`** — sees only `(Principal, ActionType, resource)`. It must not be
  given access to state or provenance, even accidentally. Enforced by constructor
  injection: it takes a `PermissionCatalog` and nothing else.
- **`TrajectoryAwareEngine`** — loads state by `workflowId`, runs the policy registry,
  applies decision precedence, and on ALLOW commits the state transition.

---

## 10. Open questions

Recorded so they are not silently resolved in our own favour:

1. Does any transformation legitimately declassify? If aggregation over 10,000 customers is
   still `CONFIDENTIAL`, the engine's false-positive rate may be unacceptable — that is
   **kill condition C**.
2. Is the total-order classification lattice sufficient, or do incomparable compartments
   change the results?
3. Can `effectiveClassification` stay sub-millisecond on a 10,000-node graph — **kill
   condition D**?
4. When a workflow spans hours and thousands of actions, does trajectory state grow without
   bound, and does any bounding strategy (windowing, summarisation) break the guarantee?
5. Does deny-overrides across many policies drive real workflows to a halt?
