# Definition of Done

Three levels: story, sprint, and the 1G phase gate.

---

## Story-level DoD

A story is done when **all** hold:

- [ ] Acceptance criteria in [BACKLOG.md](BACKLOG.md) are met
- [ ] Unit tests cover the behaviour, including the negative cases
- [ ] `./mvnw clean verify` passes from a clean clone
- [ ] No new dependency added without an ADR
- [ ] Layering rules hold (ArchUnit, once ENG-13 lands)
- [ ] Any decision that could affect the research result is recorded in an ADR or in
      `docs/results/`
- [ ] Public types have Javadoc stating *why*, not restating the signature
- [ ] The full scenario suite still passes on both engines

**Not required:** code coverage thresholds, performance targets (except ENG-24), production
hardening, or a UI.

---

## Sprint-level DoD

- [ ] Sprint goal demonstrated in running code, not slides
- [ ] The stated demo for that sprint in [SPRINTS.md](SPRINTS.md) actually runs
- [ ] Full scenario suite green through **both** engines
- [ ] Exit criteria met, or the gap written down with its consequence for the phase
- [ ] Any new kill-condition evidence recorded — **including evidence against the thesis**

---

## 1G phase gate

Transcribed from [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §42, with the verification
method for each.

| # | Criterion | Verified by |
|---|---|---|
| 1 | Spring Boot project runs | `./mvnw spring-boot:run`, `/actuator/health` UP |
| 2 | Agent simulator can generate actions | ENG-32; scenario suite executes |
| 3 | RBAC baseline works | ENG-11, ENG-12; Scenario A–H baseline column |
| 4 | Trajectory state is maintained | ENG-14, ENG-15; `GET /workflows/{id}` |
| 5 | Data classification works | ENG-04; `ClassificationTest` |
| 6 | Data provenance works at a basic level | ENG-22; `ProvenanceGraphTest` |
| 7 | Policies evaluate state + proposed action | ENG-17–ENG-21 |
| 8 | ALLOW / DENY / ASK all work | ENG-18, ENG-41; Scenario H returns ASK |
| 9 | At least 7 scenarios automated | 8 automated (A–H) |
| 10 | RBAC and trajectory results comparable | ENG-33, ENG-34; `ComparisonReport` |
| 11 | **≥1 meaningful RBAC=ALLOW / trajectory=DENY** | Scenarios E and F |
| 12 | **Decision explanations produced** | ENG-26; explanation assertions |
| 13 | **Tests document why trajectory changes authorization** | ENG-36 + `1G-RESULTS.md` |

### The stop rule

> If items 11, 12 and 13 cannot be achieved convincingly, **do not move to product
> development.**

"Convincingly" means all four hold:

1. **Realistic.** A security engineer would recognise Scenario E or F as a workflow their
   organisation might actually run.
2. **Not a permission gap.** Tests prove RBAC allowed the action because the permission
   genuinely existed, not because the baseline was built weak.
3. **Actionable explanation.** The explanation names the specific data and the specific prior
   action that caused the denial — enough for an engineer to act without reading the code.
4. **Low false positives.** Scenario G, and the ATK-12 workflows, stay ALLOW.

If item 4 fails, that is **kill condition C** and it counts even when items 1–3 pass. An
engine that denies safely by denying everything has proved nothing.

---

## Reporting standard

Applies to every result document in `docs/results/`.

- **Negative results are published with the same prominence as positive ones.** The purpose
  of 1F/1G/1H is to test a hypothesis, not to defend it.
- Scenarios B and D are reported as **plausibly already solved** by existing DLP and AI
  gateways, per [SCENARIOS.md](../docs/SCENARIOS.md). Do not present them as evidence.
- No claim that nobody is solving AI agent security. No claim to have invented
  trajectory-aware authorization. No "new category" claim.
  ([PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §35.)
- Every quantitative claim cites the run that produced it.
- The honest framing, always: *"We are testing whether enterprise authorization needs to
  become trajectory-aware as AI agents become autonomous."*
