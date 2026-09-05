# Sprint Plan — 1G and 1H

Stories are defined in [BACKLOG.md](BACKLOG.md). This file schedules them.

**Cadence:** 2-week sprints (Sprint 0 is one week).
**Assumed velocity:** ~25 points/sprint for one engineer working steadily. Adjust after
Sprint 1 — the first sprint's actual is the only velocity number worth trusting.

**Sprint start:** 2026-09-08.

---

## Milestone mapping

| Sprint | Dates | Milestone ([PROJECT_CONTEXT](../PROJECT_CONTEXT.md) §32) | Points |
|---|---|---|---|
| 0 — Foundation | Sep 8 – Sep 12 | — | 6 |
| 1 — Domain + RBAC baseline | Sep 15 – Sep 26 | G1, G2 | 26 |
| 2 — Trajectory state + policies | Sep 29 – Oct 10 | G3 | 27 |
| 3 — Provenance | Oct 13 – Oct 24 | G4 | 26 |
| 4 — Multi-agent + persistence | Oct 27 – Nov 7 | G5 | 25 |
| 5 — Experiment + 1G gate | Nov 10 – Nov 21 | G6 | 22 |
| 6–7 — 1H attack | Nov 24 – Dec 19 | 1H | 62 |

**1G decision point: 2026-11-21.** Continue to 1H, or invoke a kill condition.

---

## Sprint 0 — Foundation (1 week)

**Goal:** a running, correctly-layered app with fixtures. No authorization logic.

| Story | Title | Pts |
|---|---|---|
| ENG-01 | Spring Boot scaffold | 2 |
| ENG-02 | Package skeleton | 1 |
| ENG-03 | Fixture loading | 3 |

**Demo:** `./mvnw clean verify` green from a clean clone; `/actuator/health` UP; fixtures
load and a malformed fixture fails startup.

**Note:** you are generating `backend/` from Spring Initializr yourself. ENG-01 is done when
that lands and the build is green — the rest of Sprint 0 follows.

---

## Sprint 1 — Domain model + RBAC baseline (G1, G2)

**Goal:** the control group works end to end. No trajectory awareness anywhere yet.

Building the baseline first is deliberate — [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §26
Principle 5. It also means that if the project dies early, it dies having produced an honest
control rather than an unfalsifiable prototype.

| Story | Title | Pts |
|---|---|---|
| ENG-04 | Classification lattice | 2 |
| ENG-05 | Principal and Intent | 1 |
| ENG-06 | Action, Destination, TrustZone | 3 |
| ENG-07 | DataNode hierarchy | 3 |
| ENG-08 | Decision and result types | 2 |
| ENG-09 | Trajectory and ActionRecord | 2 |
| ENG-10 | PermissionCatalog | 2 |
| ENG-11 | RbacBaselineEngine | 3 |
| ENG-12 | Baseline isolation guarantee | 2 |
| ENG-37 | POST /authorize | 3 |
| ENG-39 | Error handling | 2 |
| | | **26** |

**Demo:** `POST /authorize` returns an RBAC ALLOW for `agent-42 / SEND_EXTERNAL` with a
reason naming the matched permission — the decision the whole project exists to challenge.

**Exit criteria**
- All domain types unit-tested, immutable, null-safe
- Baseline structurally cannot see state or provenance (ENG-12 test passes)
- Scenario A and Scenario E both return all-ALLOW under RBAC — the correct baseline answer

**Risks**
- *Domain churn.* Types will be wrong somewhere. Cheap to fix now, expensive in Sprint 3 —
  spend the time here.

---

## Sprint 2 — Trajectory state + first policies (G3)

**Goal:** decisions start depending on history. First `RBAC=ALLOW / Trajectory=DENY`.

| Story | Title | Pts |
|---|---|---|
| ENG-13 | Layering enforcement (ArchUnit) | 2 |
| ENG-14 | AuthorizationState and store | 3 |
| ENG-15 | StateTransition | 3 |
| ENG-16 | Workflow lifecycle | 2 |
| ENG-17 | Policy interface and registry | 3 |
| ENG-18 | DecisionCombiner | 2 |
| ENG-19 | ConfidentialToExternalPolicy | 3 |
| ENG-20 | ModelBoundaryPolicy | 3 |
| ENG-21 | TrajectoryAwareEngine | 5 |
| ENG-31 | Scenario model and loader | 3 |
| ENG-32 | AgentSimulator | 3 |
| | | **32** |

Over capacity at 32. **Drop first:** ENG-13 (2), then ENG-32 (3) — the harness can run from
plain JUnit fixtures for one more sprint. Do not drop ENG-21 or the sprint has no goal.

**Demo:** Scenario B — `READ customer-42` allowed, then `CALL_MODEL → openai-api` denied,
where an identical call to `internal-llm` is allowed. The engine now reads history.

**Exit criteria**
- Scenarios A, B, C pass end to end on both engines
- Trajectory engine never more permissive than baseline
- State transitions are pure and property-tested

**Risks**
- *Premature policy proliferation.* Two policies is the right number this sprint. More
  policies is not more progress — [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §41.15.
- *Client-supplied trajectory.* Reject it. If an agent can declare its own history it can
  omit the read that makes its send unsafe, and every result becomes meaningless.

---

## Sprint 3 — Provenance (G4)

**Goal:** the primitive the thesis rests on. Scenario E produces the canonical result.

**This is the highest-risk and highest-value sprint.**

| Story | Title | Pts |
|---|---|---|
| ENG-22 | ProvenanceGraph | 5 |
| ENG-23 | Graph extension on ALLOW | 3 |
| ENG-25 | ProvenanceBoundaryPolicy | 5 |
| ENG-26 | Lineage-based explanations | 5 |
| ENG-33 | ScenarioRunner | 3 |
| ENG-38 | GET /workflows/{id} | 2 |
| ENG-32 | AgentSimulator *(if carried)* | 3 |
| | | **26** |

**Demo:** Scenario E. Seven steps, RBAC allows all seven, the trajectory engine denies step 7
and explains that `report-123` derives from `pricing-strategy` (RESTRICTED) acquired at
step 2. This is the first moment the project has actual evidence.

**Exit criteria**
- Scenarios D and E produce `RBAC=ALLOW / Trajectory=DENY`
- Scenario G stays all-ALLOW — no false positives
- Explanations are generated from evidence, never templated per scenario
- `GET /workflows/{id}` shows the provenance graph

**Risks**
- *Over-classification.* If lineage makes Scenario G start failing, that is kill condition C
  arriving early. Do not weaken the test to make it pass — record it.
- *Explanation quality.* An explanation nobody can act on fails the DoD even with a correct
  decision. Budget real time for ENG-26; it is 5 points for a reason.

---

## Sprint 4 — Multi-agent + persistence (G5)

**Goal:** Scenario F — the strongest form of the hypothesis, and the case existing
per-request systems are least able to express.

| Story | Title | Pts |
|---|---|---|
| ENG-27 | Delegation model | 3 |
| ENG-28 | Cross-agent provenance | 5 |
| ENG-29 | Multi-principal trajectory | 3 |
| ENG-30 | PostgreSQL state store | 5 |
| ENG-24 | Provenance performance benchmark | 3 |
| ENG-41 | PartnerDisclosureAskPolicy | 3 |
| ENG-42 | RiskBudgetPolicy | 3 |
| | | **25** |

**Demo:** Scenario F. Agent C, which never touched the customer database and holds a valid
`SEND_EXTERNAL` permission, is denied on lineage inherited through two delegations.

**Exit criteria**
- Scenario F → `RBAC=ALLOW / Trajectory=DENY`
- Test proves agent-C's own permissions would suffice — the denial is trajectory-derived
- Every scenario passes identically against in-memory and Postgres stores
- Scenario H returns ASK
- Benchmark numbers recorded, good or bad

**Risks**
- *Scope creep into a multi-agent framework.* We model delegation as a recorded transfer of
  data ids. Nothing more. No agent runtime, no message bus.
- *Postgres becoming the sprint.* If it threatens ENG-28, cut ENG-30 to Sprint 5. The thesis
  needs Scenario F; it does not need durability.

---

## Sprint 5 — Experiment, report, and the 1G gate (G6)

**Goal:** turn a working engine into a defensible result — or an honest negative one.

| Story | Title | Pts |
|---|---|---|
| ENG-34 | ComparisonReport and metrics | 5 |
| ENG-35 | ReportRenderer | 3 |
| ENG-36 | EngineInvariantTest | 2 |
| ENG-40 | Docker and run docs | 2 |
| ENG-43 | 1G results write-up | 5 |
| ENG-44 | Kill-condition assessment | 5 |
| | | **22** |

Two stories created for this sprint only:

### ENG-43 — 1G results write-up · 5 pts · P0
`docs/results/1G-RESULTS.md`: all eight scenarios, both engines, full metrics, explanations
verbatim.
- States plainly which scenarios are thesis-evidence (E, F) and which are not (B, D)
- Reports false-positive rate from Scenario G and any others that regressed
- Includes latency and provenance-graph scaling numbers

### ENG-44 — Kill-condition assessment · 5 pts · P0
Walk [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §27 conditions A–F and answer each with
evidence from the run.
- Every condition answered met / not met / undetermined, with the evidence cited
- "Undetermined" is an acceptable answer and must say what would settle it
- Written before deciding to proceed, not after

**Demo:** the §25 transcript, produced by running code, plus the metrics table.

**Exit criteria — the 1G gate.** See [DEFINITION_OF_DONE.md](DEFINITION_OF_DONE.md). If the
last three DoD items are not convincingly met, **stop**. Per
[PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §42, do not move to product development.

---

## Sprints 6–7 — 1H attack and validation

**Goal:** break it. Then find out whether anyone else already solves it.

**Sprint 6 — attack the engine (31 pts):** ATK-01 · ATK-02 · ATK-03 · ATK-04 · ATK-08 ·
ATK-09 · ATK-12
Start with ATK-01 (provenance loss) and ATK-12 (false positives) — the two most likely to be
fatal.

**Sprint 7 — attack the thesis (31 pts):** ATK-05 · ATK-06 · ATK-07 · ATK-10 · ATK-11
ATK-11 (Cedar/Rego expressibility) is the one that decides whether there is a project here.

**Exit criteria**
- Every attack documented with outcome, whether or not it succeeded
- Comparison matrix per [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §32: what RBAC, DLP, an
  AI gateway, an existing agent-security product, and our engine each *see*, can *decide*,
  and *cannot* decide
- A written answer to: what configuration would an existing product need to reach our
  decisions on Scenarios E and F, and is that configuration natural or a custom correlation
  hack?

---

## Standing sprint mechanics

**Ceremonies:** planning at sprint start · a written mid-sprint check on whether the goal is
still reachable · demo against the sprint's stated demo, in running code · retro focused on
whether scope drifted outside 1G.

**Every sprint demo runs the full scenario suite through both engines.** A sprint that cannot
show the comparison has not demonstrated progress toward the thesis, whatever else it built.

**Carry-over rule:** an unfinished story returns to the backlog at its remaining estimate. Do
not extend a sprint — the phase gate dates are what keep the kill conditions real.

**Scope guard.** Before accepting any story into a sprint, check it against
[PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §36. If it needs a dashboard, an LLM, a broker or
a policy DSL, it is not a 1G story.
