# Backlog — Epics and Stories

Scheduling lives in [SPRINTS.md](SPRINTS.md); this file is the what and the why. Completion
criteria live in [DEFINITION_OF_DONE.md](DEFINITION_OF_DONE.md).

**Points:** modified Fibonacci (1, 2, 3, 5, 8). 1 point ≈ half a focused day.
**Total 1G:** 42 stories / 148 points. **1H:** 12 stories / 62 points.

**Priority:** `P0` blocks the 1G experiment · `P1` needed for a credible result · `P2` useful,
droppable.

---

## EPIC-1 — Foundation

*Goal: a running, testable, correctly-layered Spring Boot application.*

### ENG-01 — Spring Boot scaffold · 2 pts · P0
Generate `backend/` from Spring Initializr: group `com.aios`, artifact `authz`, Java 21,
Maven, deps `web`, `validation`, `actuator`, `test`.
- `./mvnw clean verify` succeeds from a clean clone
- App starts on :8080; `/actuator/health` returns `UP`
- No JPA/Postgres dependency yet ([ADR-0004](../docs/adr/ADR-0004-in-memory-state-store-first.md))

### ENG-02 — Package skeleton · 1 pt · P0
Create the package structure from [ARCHITECTURE.md](../docs/ARCHITECTURE.md) §3 with
`package-info.java` in each stating its responsibility and allowed dependencies.
- All packages exist and compile
- `domain` contains no Spring import

### ENG-03 — Fixture loading · 3 pts · P0
Load data assets, destinations and permissions from `resources/fixtures/*.json` at startup.
- Fixtures match the tables in [SCENARIOS.md](../docs/SCENARIOS.md)
- Malformed fixture fails startup loudly, never silently degrades
- Fixtures overridable per-test without touching production config

---

## EPIC-2 — Domain model

*Goal: the vocabulary of [DOMAIN_MODEL.md](../docs/DOMAIN_MODEL.md), fully unit-tested,
before any behaviour is built on it.*

### ENG-04 — Classification lattice · 2 pts · P0
`Classification` with ordering, `atLeast`, and `max`.
- `max` is commutative, associative, idempotent — property-tested over all pairs
- `RESTRICTED.atLeast(PUBLIC)` true; `PUBLIC.atLeast(INTERNAL)` false

### ENG-05 — Principal and Intent · 1 pt · P0
`Principal`, `PrincipalType`, `Intent`.
- Records, immutable, null-rejecting on construction

### ENG-06 — Action, Destination, TrustZone · 3 pts · P0 · **[GAP-1]**
`Action`, `ActionType`, `Destination`, `DestinationKind`, `TrustZone`.
- `destination` is never null; `Destination.NONE` for internal-only actions
- `inputDataIds` defaults to empty, never null; `outputDataId` nullable
- An internal and an external `CALL_MODEL` are distinguishable — the fix that makes
  Scenarios B and C separable

### ENG-07 — DataNode hierarchy · 3 pts · P0 · **[GAP-2]**
Sealed `DataNode` with `DataAsset` and `DerivedData`; `Transformation` enum.
- `DataAsset.derivedFrom()` returns empty set, not null
- Exhaustive switch over `DataNode` compiles without a default branch
- `Transformation` does not alter classification in 1G (asserted by test, with a comment
  pointing at the open question)

### ENG-08 — Decision and result types · 2 pts · P0 · **[GAP-3, GAP-5]**
`Decision` (ALLOW/DENY/ASK), `PolicyEvaluation`, `AuthorizationResult`.
- `PolicyEvaluation` carries `contributingDataIds` and `contributingActionIds`
- A `DENY` with empty contributing evidence is rejected at construction — this is the
  structural guarantee behind "every decision is explainable"

### ENG-09 — Trajectory and ActionRecord · 2 pts · P0
`ActionRecord`, `Trajectory` keyed by `workflowId`.
- Denied and ASKed actions are recorded, not just allowed ones
- `Trajectory` is immutable; appending returns a new instance

---

## EPIC-3 — RBAC baseline (control group)

*Goal: a faithful, non-strawman traditional authorizer. If the baseline is weak, the whole
experiment is worthless.*

### ENG-10 — PermissionCatalog · 2 pts · P0
`(principalId, ActionType, resource)` → boolean, loaded from fixtures, wildcard support.
- Supports resource wildcards (`customer-*`) as real RBAC systems do
- Unknown principal denies

### ENG-11 — RbacBaselineEngine · 3 pts · P0
Implements `AuthorizationEngine` using only the catalog.
- Returns ALLOW with a reason naming the matched permission
- Returns DENY naming the missing permission
- Never returns ASK

### ENG-12 — Baseline isolation guarantee · 2 pts · P0
The baseline must be structurally incapable of seeing trajectory or provenance.
- Constructor takes `PermissionCatalog` and nothing else
- Test asserts `RbacBaselineEngine` has no field or dependency reaching `state` or
  `provenance` packages
- **Rationale:** an accidental leak here would silently invalidate every comparison result

---

## EPIC-4 — Authorization state

*Goal: state that accumulates across a workflow, transitions purely, and is server-owned.*

### ENG-13 — Layering enforcement (ArchUnit) · 2 pts · P1
Automated test for the dependency rule in [ARCHITECTURE.md](../docs/ARCHITECTURE.md) §3.
- `domain` depends on nothing in the project
- `policy` does not depend on `api` or `engine`
- Build fails on violation

### ENG-14 — AuthorizationState and store · 3 pts · P0
`AuthorizationState`, `AuthorizationStateStore` interface, `InMemoryAuthorizationStateStore`.
- Store interface has no operation a networked store could not honour (no global iteration)
- Concurrent access to one workflow is safe

### ENG-15 — StateTransition · 3 pts · P0
Pure `apply(state, action) → state'`.
- No I/O, no clock reads — timestamps passed in
- Same inputs produce an equal output every time (property-tested)
- READ adds to `heldDataIds`; TRANSFORM/CALL_MODEL/WRITE register the output node

### ENG-16 — Workflow lifecycle · 2 pts · P0
`POST /workflows` opens a workflow with principal + intent, returns `workflowId`.
- Intent is immutable for the workflow's lifetime
- Authorizing against an unknown `workflowId` returns 404, never an implicit new workflow

---

## EPIC-5 — Policy engine

*Goal: composable, stateless, independently testable policies.*

### ENG-17 — Policy interface and registry · 3 pts · P0
`Policy`, `PolicyRegistry` with Spring-discovered `List<Policy>`.
- Adding a policy requires no edit to the engine or the registry
- `appliesTo` false ⇒ policy omitted from evaluations rather than returning vacuous ALLOW

### ENG-18 — DecisionCombiner · 2 pts · P0 · **[GAP-3]**
Deny-overrides: `DENY > ASK > ALLOW`.
- Order-independent — property-tested over shuffled evaluation lists
- Empty evaluation list ⇒ ALLOW (nothing objected)

### ENG-19 — ConfidentialToExternalPolicy · 3 pts · P0
Held data at `CONFIDENTIAL`+ may not reach `TrustZone.EXTERNAL`.
- Satisfies Scenario D step 4
- Cites the specific data ids and the acquiring action ids

### ENG-20 — ModelBoundaryPolicy · 3 pts · P0 · **[GAP-1]**
`CALL_MODEL` to an external model is a boundary crossing.
- Scenario B step 2 → DENY; Scenario C step 2 → ALLOW
- Test asserts the two differ *only* by destination trust zone

### ENG-21 — TrajectoryAwareEngine · 5 pts · P0
Load state → RBAC precheck → registry → combine → commit on ALLOW.
- Never converts an RBAC DENY into ALLOW
- State is committed only on ALLOW; DENY records the attempt without extending provenance
- Returns every evaluation, including abstentions, in the result

---

## EPIC-6 — Provenance

*Goal: the primitive the thesis rests on.*

### ENG-22 — ProvenanceGraph · 5 pts · P0 · **[GAP-2]**
`effectiveClassification`, `rootsOf`, `lineagePath`.
- Least upper bound over the transitive closure of `derivedFrom`
- Cycle-safe (an agent may write a derived artefact back to a source system)
- Depth-10 chain from `RESTRICTED` still resolves `RESTRICTED`

### ENG-23 — Graph extension on ALLOW · 3 pts · P0
An allowed action with an `outputDataId` registers a `DerivedData` node linked to its inputs.
- Derived nodes are created by the engine, never trusted from the client
- DENY and ASK do not extend the graph

### ENG-24 — Provenance performance benchmark · 3 pts · P1
Measure `effectiveClassification` against graph size — **kill condition D**.
- Report p50/p95/p99 at 10 / 100 / 1,000 / 10,000 nodes
- Records the shape of the curve, not just a pass/fail number
- Result written to `docs/results/` regardless of outcome, including a bad one

### ENG-25 — ProvenanceBoundaryPolicy · 5 pts · P0
Deny when *effective* (lineage-derived) classification exceeds the destination zone maximum.
- Satisfies Scenarios D, E and F — the primary evidence
- Distinguishable from `ConfidentialToExternalPolicy`: fires on derived data holding no
  directly-read confidential asset

### ENG-26 — Lineage-based explanations · 5 pts · P0
`ExplanationBuilder` generates prose from evaluation evidence.
- Names the dominant contributing asset, its classification, and the acquiring step
- Contains identifiers and classifications only — never data content
- Generated from `contributingDataIds`/`contributingActionIds`, never templated per-scenario
- Scenario E's explanation passes all four assertions in [SCENARIOS.md](../docs/SCENARIOS.md)

---

## EPIC-7 — Multi-agent propagation

*Goal: Scenario F — the strongest form of the hypothesis.*

### ENG-27 — Delegation model · 3 pts · P0 · **[GAP-4]**
`Delegation` record; `POST /workflows/{id}/delegate`.
- Delegation names the transferred data ids explicitly
- Delegating data the workflow does not hold is rejected

### ENG-28 — Cross-agent provenance · 5 pts · P0
Provenance is workflow-scoped, so a delegate inherits upstream lineage.
- Scenario F step 5 → DENY
- Test asserts `agent-C`'s permissions alone would ALLOW, proving the denial is trajectory-
  derived and not a permission gap

### ENG-29 — Multi-principal trajectory · 3 pts · P1
`ActionRecord` carries the acting principal; explanations name which agent did what.
- Scenario F explanation names agent-A as the acquirer and agent-C as the sender

### ENG-30 — PostgreSQL state store · 5 pts · P1
JPA implementation of `AuthorizationStateStore`; `docker-compose.yml` for local Postgres.
- Every scenario passes against both stores, unchanged
- In-memory remains the test default for speed

---

## EPIC-8 — Agent harness

*Goal: the rig that turns the thesis into a repeatable experiment. This is the deliverable
that produces the evidence — not a test utility.*

### ENG-31 — Scenario model and loader · 3 pts · P0
`Scenario`, `ScenarioStep`, JSON loader for `resources/scenarios/*.json`.
- A scenario declares steps with expected RBAC and trajectory decisions
- Adding a scenario means adding one JSON file, no code
- Malformed scenario fails loudly

### ENG-32 — AgentSimulator · 3 pts · P0
Replays scenario steps as `AuthorizationRequest`s against an engine.
- Deterministic, no randomness, no LLM
- Continues after a DENY so later steps are still observed (a stop-on-first-deny harness
  would hide exactly the behaviour we are studying)
- Supports multi-principal scenarios for Scenario F

### ENG-33 — ScenarioRunner · 3 pts · P0
Runs one scenario through both engines on identical input and pairs the decisions.
- Both engines receive byte-identical requests
- Per-step timing captured for both

### ENG-34 — ComparisonReport and metrics · 5 pts · P0
Computes the metrics from [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §23.
- Dangerous actions prevented, false positives, latency, approvals required, completion rate
- False positives measured against Scenario G specifically
- Machine-readable output written to `docs/results/`

### ENG-35 — ReportRenderer · 3 pts · P1
Renders the §25 killer-demo transcript to stdout.
- Reproduces the §25 format for Scenario E
- Shows both decisions side by side with the generated reason
- Plain text, no UI

### ENG-36 — EngineInvariantTest · 2 pts · P0
Cross-scenario invariants.
- Trajectory decisions are never more permissive than RBAC, on every step of every scenario
- Every DENY carries non-empty contributing evidence
- Re-running a scenario produces identical decisions (determinism)

---

## EPIC-9 — API and operations

### ENG-37 — POST /authorize · 3 pts · P0
- Bean-validated request; 400 on malformed, 404 on unknown workflow
- Response matches [ARCHITECTURE.md](../docs/ARCHITECTURE.md) §4
- Client-supplied trajectory is rejected, not merged (forgery prevention)

### ENG-38 — GET /workflows/{id} · 2 pts · P1
Inspect trajectory, provenance and decision log. For debugging and 1H, not for a UI.

### ENG-39 — Error handling · 2 pts · P1
`GlobalExceptionHandler` with consistent problem responses.
- No stack traces or internal ids leak to clients

### ENG-40 — Docker and run docs · 2 pts · P2
`Dockerfile`, compose file, verified quickstart in [README.md](../README.md).

---

## EPIC-10 — ASK and risk budget

### ENG-41 — PartnerDisclosureAskPolicy · 3 pts · P1 · **[GAP-3]**
`CONFIDENTIAL` → `PARTNER` under matching intent returns ASK rather than DENY.
- Scenario H → ASK
- Does not weaken Scenario E, where `RESTRICTED` still denies
- **Why it matters:** without ASK, the only lever against false positives is loosening
  policy, which would make kill condition C artificially easy to hit

### ENG-42 — RiskBudgetPolicy · 3 pts · P2
Per-workflow risk budget; irreversible actions cost more.
- Exhausted budget returns ASK, not DENY
- Budget spend appears in the decision log

---

## EPIC-11 — 1H attack and validation

*Goal: break our own engine. Per [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §31 — "the
objective is not to make the prototype look good."*

**Do not start until the 1G gate in [DEFINITION_OF_DONE.md](DEFINITION_OF_DONE.md) passes.**

| ID | Attack | Pts | Kill condition |
|---|---|---|---|
| ATK-01 | Provenance loss — agent reads via an unmediated path | 5 | D |
| ATK-02 | Trajectory reset — new workflow to shed accumulated state | 5 | — |
| ATK-03 | Malicious tool falsifies classification metadata | 5 | — |
| ATK-04 | Out-of-band agent-to-agent transfer bypassing `DELEGATE` | 5 | — |
| ATK-05 | Transformation laundering — chain transforms to shed classification | 8 | — |
| ATK-06 | Allowed-destination abuse — exfiltrate via a permitted internal sink | 5 | — |
| ATK-07 | Tool chaining to reach an external effect without `SEND_EXTERNAL` | 5 | — |
| ATK-08 | Prompt injection alters declared intent | 3 | E |
| ATK-09 | Policy gaps — action types no policy claims | 3 | — |
| ATK-10 | Evaluation cost blow-up on a pathological graph | 5 | D |
| ATK-11 | Express Scenarios E and F in Cedar and Rego | 8 | **A** |
| ATK-12 | False-positive hunt on realistic benign workflows | 5 | **C** |

**ATK-11 is the most important story in the project.** If Scenarios E and F are naturally
expressible in Cedar or Rego without an external correlation store feeding them accumulated
trajectory, kill condition A is met and the thesis is dead. That finding must be published in
`docs/results/` as prominently as a positive one.

---

## Out of scope for 1G

Rejected on sight, per [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §36: dashboard, chatbot,
LLM policy reasoning, OPA/Cedar integration (except ATK-11's comparison), Kubernetes, SSO,
message broker, multi-tenancy, "AI OS" branding, hundreds of policies.
