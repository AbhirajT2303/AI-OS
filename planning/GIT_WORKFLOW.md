# Git Workflow

How backlog stories ([BACKLOG.md](BACKLOG.md)) and sprints ([SPRINTS.md](SPRINTS.md)) map
onto branches, merges, and commit messages.

---

## 1. Branching model

**One short-lived branch per story, not per sprint.**

```text
main ──●──●──●───────────────────────────────────▶
        \         \
         ● (branch) ● (branch)
     story/eng-04-…   story/eng-06-…
```

- Branch from `main`, named `story/<id>-<slug>`:
  - `story/eng-04-classification-lattice`
  - `story/eng-21-trajectory-aware-engine`
  - `story/atk-11-cedar-rego-expressibility` (1H attack stories use the same pattern)
- Work the story, keep `main` untouched until it's done.
- Merge back to `main` only when the story's DoD in
  [DEFINITION_OF_DONE.md](DEFINITION_OF_DONE.md) is met — build green, tests written, full
  scenario suite still passing.
- Delete the branch after merge. Branches are disposable; the commit and the backlog entry
  are the durable record.

**Why not sprint branches:** a sprint is a calendar checkpoint, not a unit of work — see
[SPRINTS.md](SPRINTS.md)'s carry-over rule, where an unfinished story just returns to the
backlog. A `sprint-2` branch would accumulate merge debt every time that happens. Stories are
the atomic, mergeable unit; sprints are how they're scheduled, not how they're branched.

**Why not a `develop` branch:** this is a research prototype with one active line of work per
DEFINITION_OF_DONE.md's phase gates. An integration branch adds a merge step with nothing on
the other side of it yet. Reconsider if 1H parallelizes across multiple attack stories at
once (Sprints 6–7 already schedule ATK stories to be worked one at a time, so likely
unnecessary even then).

---

## 2. Merging: squash, not merge-commit

```bash
git checkout main
git merge --squash story/eng-04-classification-lattice
git commit   # write the message per §3
git branch -d story/eng-04-classification-lattice
```

**Why squash:**
- `main`'s history becomes exactly one commit per backlog story — `git log --oneline` reads
  as a changelog against [BACKLOG.md](BACKLOG.md) with no reconstruction needed.
- Avoids merge-commit noise from a solo contributor's own false-starts and fixups inside a
  branch — those belong in the branch, not in `main`'s permanent history.

**Exception:** if a story is large enough that its internal commit history is itself worth
preserving (e.g. ENG-22 ProvenanceGraph, ENG-34 ComparisonReport), say so explicitly and use
a regular merge (`--no-ff`) instead. Default to squash otherwise.

---

## 3. Commit message convention

[Conventional Commits](https://www.conventionalcommits.org/) type/scope, plus a `Story:`
trailer that ties the commit back to its backlog entry.

```text
<type>(<scope>): <imperative summary, ≤72 chars>

<body — the why, not the what. Cite the specific DoD criteria satisfied,
 or the finding if this is a 1H attack story. Wrap at ~72 chars.>

Story: ENG-04
```

**`type`** — one of:

| Type | Use for |
|---|---|
| `feat` | A new capability: a domain type, a policy, an endpoint |
| `fix` | Correcting a defect in already-merged work |
| `test` | Tests only, no production code change |
| `docs` | Changes under `docs/` or `planning/` only |
| `refactor` | No behavior change |
| `perf` | Performance-motivated change (ENG-24 and similar) |
| `chore` | Build, tooling, dependency bumps |

**`scope`** — the package or area touched: `domain`, `provenance`, `state`, `policy`,
`engine`, `harness`, `api`, `config`, `docs`, `planning`.

**`Story:`** — the exact id from [BACKLOG.md](BACKLOG.md) (`ENG-04`, `ATK-11`). One story per
commit wherever possible; if a commit genuinely spans two, list both (`Story: ENG-19,
ENG-20`). This is what makes `git log --grep "Story: ENG-21"` a complete history of one
story regardless of which sprint it landed in.

### Examples

```text
feat(domain): add Classification lattice with atLeast/max

Total order over PUBLIC < INTERNAL < CONFIDENTIAL < RESTRICTED. max is
commutative/associative/idempotent, property-tested over all pairs —
this is the primitive ProvenanceGraph's effectiveClassification will
build on (ENG-22).

Story: ENG-04
```

```text
test(policy): property-test DecisionCombiner order independence

Shuffles the evaluation list across 200 random permutations per case
to confirm deny-overrides doesn't depend on registration order — this
is what keeps policy composition safe as rules are added later.

Story: ENG-18
```

```text
docs(planning): record ATK-11 finding — Cedar expresses Scenario E

Scenario E is naturally expressible in Cedar given a pre-populated
entity graph carrying provenance. The entity graph itself still needs
external maintenance Cedar doesn't provide — see docs/results/1H-ATK-11.md
for the full comparison. Bears on kill condition A.

Story: ATK-11
Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
```

A `docs`/`ATK-*` commit like the third example is exactly where a **negative** result gets
recorded — per [DEFINITION_OF_DONE.md](DEFINITION_OF_DONE.md)'s reporting standard, it goes
in with the same weight as a positive one.

---

## 4. Sprint checkpoints: tags, not branches

Tag `main` at each sprint's demo point once its exit criteria in
[SPRINTS.md](SPRINTS.md) are met:

```bash
git tag -a sprint-0 -m "Sprint 0 demo: scaffold + fixtures, /actuator/health green"
git push origin sprint-0
```

Planned tags: `sprint-0` … `sprint-5`, `1g-gate` (the phase-gate decision point), `sprint-6`,
`sprint-7`, `1h-gate`. A tag costs nothing to maintain and gives a durable link between the
sprint plan and actual git history without the staleness a sprint-numbered branch would
accumulate.

---

## 5. What predates this convention

The first three commits on `main` (`first commit`, the backend scaffold, ENG-02/ENG-03) were
made directly to `main` before this document existed. Not rewriting that history for it.
Sprint 0's exit criteria are already met by that point, so it gets tagged `sprint-0` as-is —
starting from **ENG-04 onward**, every story goes through a `story/*` branch, squash-merge,
and the `Story:` trailer.
