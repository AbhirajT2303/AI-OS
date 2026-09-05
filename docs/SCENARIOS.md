# Scenarios A–G — Executable Specification

These are the seven scenarios required by [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §24,
formalised against [DOMAIN_MODEL.md](DOMAIN_MODEL.md) so each one can be a JSON fixture and
an automated test.

Every scenario runs through **both** engines. The `RBAC` column is the control group; the
experiment is the delta.

---

## Fixture data assets

Shared by all scenarios (`resources/fixtures/data-assets.json`):

| id | classification | source |
|---|---|---|
| `public-brochure` | PUBLIC | `marketing-cms` |
| `customer-42` | CONFIDENTIAL | `customer-db` |
| `pricing-strategy` | RESTRICTED | `pricing-db` |
| `support-tickets-42` | INTERNAL | `support-db` |
| `partner-nda-terms` | INTERNAL | `contracts-db` |

## Fixture destinations

| id | kind | trustZone |
|---|---|---|
| `internal-llm` | MODEL | INTERNAL |
| `openai-api` | MODEL | EXTERNAL |
| `partner.com` | EMAIL | PARTNER |
| `attacker.example` | HTTP | EXTERNAL |
| `report-store` | DATASTORE | INTERNAL |

## Fixture permissions (RBAC catalog)

`agent-42` holds `READ`, `WRITE`, `CALL_TOOL`, `CALL_MODEL`, `TRANSFORM`, `SEND_EXTERNAL`,
`DELEGATE` on all listed resources. **This is intentional.** If the baseline denies for lack
of permission, the scenario proves nothing — the interesting cases are the ones where RBAC
has no reason to object.

---

## Scenario A — Public data, external send

**Purpose:** the engine must not be trivially restrictive.

| # | Actor | Action | Destination | RBAC | Trajectory |
|---|---|---|---|---|---|
| 1 | agent-42 | READ `public-brochure` | — | ALLOW | ALLOW |
| 2 | agent-42 | CALL_MODEL | `openai-api` | ALLOW | ALLOW |
| 3 | agent-42 | SEND_EXTERNAL | `attacker.example` | ALLOW | **ALLOW** |

Effective classification at step 3 is `PUBLIC`. No boundary is crossed.

> Note the destination is deliberately hostile-looking. The engine has no opinion about
> reputation — only about information flow. If we ever want reputation, that is a different
> product and a different thesis.

---

## Scenario B — Confidential data to an external model **[resolves GAP-1]**

| # | Actor | Action | Destination | RBAC | Trajectory |
|---|---|---|---|---|---|
| 1 | agent-42 | READ `customer-42` | — | ALLOW | ALLOW |
| 2 | agent-42 | CALL_MODEL | `openai-api` (EXTERNAL) | ALLOW | **DENY** |

Fired by `ModelBoundaryPolicy`: held data has effective classification `CONFIDENTIAL`;
maximum for `TrustZone.EXTERNAL` is `PUBLIC`.

**Honesty note:** [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §9 Test 1 concluded this case
is *not sufficiently unique* — DLP and AI gateways already do it. It is in the suite as a
correctness baseline, **not** as evidence for the thesis. Do not put this scenario in a
demo.

---

## Scenario C — Confidential data to an approved internal model

**Purpose:** false-positive control. The engine must not deny merely because confidential
data is present.

| # | Actor | Action | Destination | RBAC | Trajectory |
|---|---|---|---|---|---|
| 1 | agent-42 | READ `customer-42` | — | ALLOW | ALLOW |
| 2 | agent-42 | CALL_MODEL → `analysis-1` | `internal-llm` (INTERNAL) | ALLOW | **ALLOW** |
| 3 | agent-42 | WRITE `internal-report-1` | `report-store` (INTERNAL) | ALLOW | **ALLOW** |

`internal-report-1` now carries effective classification `CONFIDENTIAL` by lineage — which
is what makes Scenario D fire.

---

## Scenario D — Confidential → internal report → external email

**Purpose:** the first case where content inspection alone is insufficient.

| # | Actor | Action | Destination | RBAC | Trajectory |
|---|---|---|---|---|---|
| 1 | agent-42 | READ `customer-42` | — | ALLOW | ALLOW |
| 2 | agent-42 | CALL_MODEL → `summary-1` | `internal-llm` | ALLOW | ALLOW |
| 3 | agent-42 | WRITE `internal-report-1` | `report-store` | ALLOW | ALLOW |
| 4 | agent-42 | SEND_EXTERNAL `internal-report-1` | `partner.com` | ALLOW | **DENY** |

Steps 1–3 are individually and collectively fine. Step 4 is denied on lineage:
`internal-report-1 ← summary-1 ← customer-42 (CONFIDENTIAL)`.

Note the report may contain no literal customer string at all. That is the point, and it is
where this diverges from content-scanning DLP.

---

## Scenario E — Multi-source report (the canonical scenario)

**This is the demo.** [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §8 and §25.

Intent: `prepare_partner_report`.

| # | Actor | Action | Destination | RBAC | Trajectory |
|---|---|---|---|---|---|
| 1 | agent-42 | READ `customer-42` | — | ALLOW | ALLOW |
| 2 | agent-42 | READ `pricing-strategy` | — | ALLOW | ALLOW |
| 3 | agent-42 | READ `support-tickets-42` | — | ALLOW | ALLOW |
| 4 | agent-42 | TRANSFORM → `analytics-1` (AGGREGATE) | — | ALLOW | ALLOW |
| 5 | agent-42 | CALL_MODEL → `draft-1` | `internal-llm` | ALLOW | ALLOW |
| 6 | agent-42 | WRITE `report-123` | `report-store` | ALLOW | ALLOW |
| 7 | agent-42 | SEND_EXTERNAL `report-123` | `partner.com` | **ALLOW** | **DENY** |

```text
report-123                            effective = RESTRICTED
├── draft-1
│   └── analytics-1
│       ├── customer-42        CONFIDENTIAL
│       ├── pricing-strategy   RESTRICTED   ◀── the reason
│       └── support-tickets-42 INTERNAL
```

Required explanation quality — the test asserts all four:

1. names `pricing-strategy` as the dominant contributor,
2. names step 2 as the action that acquired it,
3. states the effective classification `RESTRICTED` and the `PARTNER` zone maximum,
4. contains no data content, only identifiers and classifications.

---

## Scenario F — Multi-agent propagation **[resolves GAP-4]**

**Purpose:** the case existing per-request systems are least able to express, because no
single request contains the evidence.

Workflow `wf-multi`, intent `partner_briefing`.

| # | Actor | Action | Destination | RBAC | Trajectory |
|---|---|---|---|---|---|
| 1 | agent-A | READ `customer-42` | — | ALLOW | ALLOW |
| 2 | agent-A | DELEGATE → agent-B, transfers `customer-42` | AGENT | ALLOW | ALLOW |
| 3 | agent-B | CALL_MODEL → `summary-2` | `internal-llm` | ALLOW | ALLOW |
| 4 | agent-B | DELEGATE → agent-C, transfers `summary-2` | AGENT | ALLOW | ALLOW |
| 5 | agent-C | SEND_EXTERNAL `summary-2` | `partner.com` | **ALLOW** | **DENY** |

`agent-C` never touched `customer-db` and holds a valid `SEND_EXTERNAL` permission. RBAC
sees a clean request. The workflow-scoped provenance graph sees
`summary-2 ← customer-42 (CONFIDENTIAL)`.

Additional assertion: `agent-C`'s own permission set alone is sufficient for ALLOW, proving
the denial comes from trajectory and not from a permission gap.

---

## Scenario G — Benign multi-step workflow

**Purpose:** false-positive measurement. Without this, a `DENY`-everything engine scores
perfectly.

| # | Actor | Action | Destination | RBAC | Trajectory |
|---|---|---|---|---|---|
| 1 | agent-42 | READ `support-tickets-42` (INTERNAL) | — | ALLOW | ALLOW |
| 2 | agent-42 | CALL_TOOL → `triage-1` | internal tool | ALLOW | ALLOW |
| 3 | agent-42 | CALL_MODEL → `summary-3` | `internal-llm` | ALLOW | ALLOW |
| 4 | agent-42 | WRITE `internal-summary` | `report-store` | ALLOW | ALLOW |
| 5 | agent-42 | READ `partner-nda-terms` | — | ALLOW | ALLOW |
| 6 | agent-42 | WRITE `internal-brief` | `report-store` | ALLOW | **ALLOW** |

Six steps, internal data, no boundary crossed. **Every step must ALLOW.** A single spurious
DENY here is a false positive and counts against kill condition C.

---

## Scenario H — Partner disclosure requiring approval *(stretch)*

Exercises `ASK`, which no other scenario covers.

| # | Actor | Action | Destination | RBAC | Trajectory |
|---|---|---|---|---|---|
| 1 | agent-42 | READ `customer-42` (CONFIDENTIAL) | — | ALLOW | ALLOW |
| 2 | agent-42 | WRITE `partner-summary` | `report-store` | ALLOW | ALLOW |
| 3 | agent-42 | SEND_EXTERNAL `partner-summary` | `partner.com` (PARTNER) | ALLOW | **ASK** |

`CONFIDENTIAL` (not `RESTRICTED`) to `PARTNER` (not `EXTERNAL`) under a matching declared
intent is exactly the case a human should adjudicate. Compare with Scenario E, where
`RESTRICTED` makes it an outright DENY.

> **Resolved in ENG-41 (Sprint 4):** Scenario D and Scenario H are both "`CONFIDENTIAL` data
> reaches `PARTNER`", distinguished only by `Intent`. Scenario H's intent id is
> `approved-partner-disclosure` — a fixed, explicit allowlist `PartnerDisclosureAskPolicy`
> checks — while Scenario D's `partner-report` intent isn't on it. `ProvenanceBoundaryPolicy`
> steps aside (ALLOWs instead of denying) for exactly that CONFIDENTIAL+PARTNER+approved-intent
> cell, letting `PartnerDisclosureAskPolicy` contribute the ASK; every other cell, including
> Scenario E's `RESTRICTED`, is unaffected and still denies outright.

---

## Summary — the experiment

| Scenario | RBAC | Trajectory | Thesis-relevant? |
|---|---|---|---|
| A — public data | ALLOW | ALLOW | control |
| B — confidential → external model | ALLOW | DENY | **no** — existing DLP does this |
| C — confidential → internal model | ALLOW | ALLOW | control |
| D — report → external email | ALLOW | DENY | partial — content DLP may catch it |
| **E — multi-source report** | **ALLOW** | **DENY** | **yes — primary evidence** |
| **F — multi-agent propagation** | **ALLOW** | **DENY** | **yes — strongest evidence** |
| G — benign workflow | ALLOW | ALLOW | **false-positive control** |
| H — partner disclosure | ALLOW | ASK | stretch |

Only **E** and **F** are evidence for the thesis. B and D must be reported honestly as cases
existing products can plausibly already handle — claiming them as novel would be exactly the
overreach [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §35 warns against.

For each of E and F, 1H must answer: *what configuration would an existing product need to
reach the same decision, and is that configuration natural or a custom correlation hack?*
