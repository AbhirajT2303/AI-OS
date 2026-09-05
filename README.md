# AI-OS — Trajectory-Aware Authorization

Research prototype testing one claim:

> An autonomous AI workflow can contain individually authorized actions that become
> unauthorized when evaluated as a complete trajectory.

This is a **research/validation project**, not a product. The thesis is a hypothesis under
test and may be killed. See [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) §27 for the kill
conditions.

**Current phase:** `1G — Build Authorization Engine`

---

## Repository map

| Path | Purpose |
|---|---|
| [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) | Canonical engineering context. Read first. |
| [AI_OS_Conversation_History.md](AI_OS_Conversation_History.md) | How the thesis was reached (background, superseded by the above where they differ). |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Components, package layout, request flow. |
| [docs/DOMAIN_MODEL.md](docs/DOMAIN_MODEL.md) | The resolved domain model — supersedes the sketches in PROJECT_CONTEXT §13–20. |
| [docs/SCENARIOS.md](docs/SCENARIOS.md) | Scenarios A–G as an executable specification. |
| [docs/adr/](docs/adr/) | Architecture decision records. |
| [planning/BACKLOG.md](planning/BACKLOG.md) | Epics and stories with acceptance criteria. |
| [planning/SPRINTS.md](planning/SPRINTS.md) | Sprint plan, G1–G6 milestone mapping. |
| [planning/DEFINITION_OF_DONE.md](planning/DEFINITION_OF_DONE.md) | Story-level and phase-level DoD. |
| [planning/GIT_WORKFLOW.md](planning/GIT_WORKFLOW.md) | Branching, merging, commit message convention. |
| `backend/` | Spring Boot application. |

---

## The experiment

Every scenario runs through **two engines** on the same input:

```text
                     ┌──────────────────────┐
   Scenario  ───────▶│   RBAC Baseline      │──▶ Decision + reason
   (fixture)         └──────────────────────┘
        │
        │            ┌──────────────────────┐
        └───────────▶│  Trajectory Engine   │──▶ Decision + reason + evidence
                     └──────────────────────┘
                                │
                                ▼
                        ComparisonReport
```

The baseline is not optional. A trajectory-aware DENY only means something next to an
RBAC ALLOW on identical input.

**1G succeeds** when at least one realistic scenario produces `RBAC=ALLOW` /
`Trajectory=DENY`, with a machine-generated explanation naming the data and actions that
caused the denial. See [planning/DEFINITION_OF_DONE.md](planning/DEFINITION_OF_DONE.md).

---

## Non-negotiable constraints

These come from [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) §41 and are enforced in review:

1. **No LLM in the policy path.** Decisions are deterministic. See [ADR-0001](docs/adr/ADR-0001-no-llm-in-the-policy-decision-path.md).
2. **No UI** until the engine is proven.
3. **Preserve the RBAC baseline.** Never demonstrate the new engine alone.
4. **Every decision is explainable** — which policy fired, on what evidence.
5. **Provenance stays separate from raw content.** We reason over lineage, not text.
6. Do not broaden into a generic AI security platform. Do not use "AI OS" branding.
7. If an implementation decision would materially change the hypothesis, stop and surface it.

---

## Getting started

```bash
cd backend
./mvnw spring-boot:run          # API on :8080
./mvnw test                     # unit + scenario tests
./mvnw test -Dtest=ScenarioComparisonTest   # the experiment
```

Requires Java 21+.
