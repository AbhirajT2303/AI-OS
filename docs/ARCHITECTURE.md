# Architecture — 1G Authorization Engine

Companion to [DOMAIN_MODEL.md](DOMAIN_MODEL.md). This document covers components, the
request path, and the concrete package layout for `backend/`.

---

## 1. Components

```text
        ┌──────────────────────────────────────────────────┐
        │                  Agent Harness                   │
        │                                                  │
        │   ScenarioLibrary ──▶ AgentSimulator             │
        │          │                   │                   │
        │          │            emits ActionRequests       │
        │          ▼                   ▼                   │
        │   ScenarioRunner ──▶ both engines ──▶ Comparison │
        └───────────────────────┬──────────────────────────┘
                                │
                                ▼
                  ┌───────────────────────────┐
                  │   AuthorizationController  │
                  │   POST /authorize          │
                  │   POST /workflows          │
                  │   GET  /workflows/{id}     │
                  └─────────────┬─────────────┘
                                │
              ┌─────────────────┴─────────────────┐
              ▼                                   ▼
   ┌────────────────────┐            ┌──────────────────────────┐
   │ RbacBaselineEngine │            │  TrajectoryAwareEngine   │
   │                    │            │                          │
   │ PermissionCatalog  │            │  1. load state           │
   │ only               │            │  2. RBAC precheck        │
   │                    │            │  3. run PolicyRegistry   │
   │ (no state access)  │            │  4. combine (deny-wins)  │
   └────────────────────┘            │  5. commit transition    │
                                     └───────────┬──────────────┘
                                                 │
                    ┌────────────────────────────┼────────────────────┐
                    ▼                            ▼                    ▼
        ┌───────────────────────┐   ┌──────────────────┐   ┌──────────────────┐
        │ AuthorizationStateStore│   │  PolicyRegistry  │   │ ProvenanceService│
        │ (workflowId → state)   │   │  List<Policy>    │   │ graph + LUB      │
        └───────────────────────┘   └──────────────────┘   └──────────────────┘
```

The **Executor** from [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §12 is simulated by the
harness in 1G. Nothing real is executed; we are testing the decision, not the side effect.

---

## 2. Request path

```text
POST /authorize
   │
   ├─▶ validate request                          400 on malformed
   │
   ├─▶ load AuthorizationState by workflowId     404 if workflow unknown
   │
   ├─▶ RBAC precheck ──── DENY ────────────────▶ return DENY (no permission)
   │        │
   │      ALLOW
   │        ▼
   ├─▶ for each Policy where appliesTo(state, action):
   │        evaluate() ──▶ PolicyEvaluation
   │
   ├─▶ combine: DENY > ASK > ALLOW
   │
   ├─▶ if ALLOW: state' = StateTransition.apply(state, action); persist
   │   if DENY:  record the denied action in trajectory; do not extend provenance
   │   if ASK:   record as pending; do not extend provenance
   │
   └─▶ return AuthorizationResult { decision, explanation, evaluations, timing }
```

**Trajectory-aware never overrides an RBAC DENY into an ALLOW.** The new engine can only be
more restrictive than the baseline. That property is asserted by a test over every scenario
— if it ever fails, the comparison is meaningless.

---

## 3. Package layout for `backend/`

Generate from Spring Initializr with group `com.aios`, artifact `authz`, Java 21, Maven, and
dependencies: `spring-boot-starter-web`, `spring-boot-starter-validation`,
`spring-boot-starter-actuator`, `spring-boot-starter-test`. Add `spring-boot-starter-data-jpa`
and `postgresql` only when persistence lands (Sprint 4) — see
[ADR-0004](adr/ADR-0004-in-memory-state-store-first.md).

```text
backend/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
└── src/
    ├── main/
    │   ├── java/com/aios/authz/
    │   │   ├── AuthzApplication.java
    │   │   │
    │   │   ├── domain/                     # pure types, zero Spring, zero I/O
    │   │   │   ├── Principal.java
    │   │   │   ├── PrincipalType.java
    │   │   │   ├── Intent.java
    │   │   │   ├── Classification.java
    │   │   │   ├── TrustZone.java
    │   │   │   ├── DataNode.java           # sealed
    │   │   │   ├── DataAsset.java
    │   │   │   ├── DerivedData.java
    │   │   │   ├── Transformation.java
    │   │   │   ├── Action.java
    │   │   │   ├── ActionType.java
    │   │   │   ├── Destination.java
    │   │   │   ├── DestinationKind.java
    │   │   │   ├── ActionRecord.java
    │   │   │   ├── Delegation.java
    │   │   │   ├── Trajectory.java
    │   │   │   ├── Capability.java
    │   │   │   ├── RiskBudget.java
    │   │   │   └── Decision.java
    │   │   │
    │   │   ├── provenance/
    │   │   │   ├── ProvenanceGraph.java
    │   │   │   └── ProvenanceService.java
    │   │   │
    │   │   ├── state/
    │   │   │   ├── AuthorizationState.java
    │   │   │   ├── StateTransition.java     # pure function
    │   │   │   ├── AuthorizationStateStore.java
    │   │   │   └── InMemoryAuthorizationStateStore.java
    │   │   │
    │   │   ├── policy/
    │   │   │   ├── Policy.java
    │   │   │   ├── PolicyEvaluation.java
    │   │   │   ├── PolicyRegistry.java
    │   │   │   ├── DecisionCombiner.java
    │   │   │   └── rules/
    │   │   │       ├── ConfidentialToExternalPolicy.java
    │   │   │       ├── ModelBoundaryPolicy.java
    │   │   │       ├── ProvenanceBoundaryPolicy.java
    │   │   │       ├── PartnerDisclosureAskPolicy.java
    │   │   │       └── RiskBudgetPolicy.java
    │   │   │
    │   │   ├── engine/
    │   │   │   ├── AuthorizationEngine.java
    │   │   │   ├── RbacBaselineEngine.java
    │   │   │   ├── TrajectoryAwareEngine.java
    │   │   │   ├── PermissionCatalog.java
    │   │   │   └── ExplanationBuilder.java
    │   │   │
    │   │   ├── harness/                     # the agent harness
    │   │   │   ├── AgentSimulator.java
    │   │   │   ├── Scenario.java
    │   │   │   ├── ScenarioStep.java
    │   │   │   ├── ScenarioLoader.java
    │   │   │   ├── ScenarioRunner.java
    │   │   │   ├── ComparisonReport.java
    │   │   │   └── ReportRenderer.java
    │   │   │
    │   │   ├── api/
    │   │   │   ├── AuthorizationController.java
    │   │   │   ├── WorkflowController.java
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   └── dto/
    │   │   │       ├── AuthorizeRequestDto.java
    │   │   │       ├── AuthorizeResponseDto.java
    │   │   │       └── OpenWorkflowDto.java
    │   │   │
    │   │   └── config/
    │   │       ├── PolicyConfig.java
    │   │       └── FixtureConfig.java        # seed catalog + data assets
    │   │
    │   └── resources/
    │       ├── application.properties
    │       ├── fixtures/
    │       │   ├── data-assets.json
    │       │   └── permissions.json
    │       └── scenarios/
    │           ├── scenario-a-public-data.json
    │           ├── scenario-b-confidential-external-model.json
    │           ├── scenario-c-confidential-private-model.json
    │           ├── scenario-d-report-external-email.json
    │           ├── scenario-e-multi-source.json
    │           ├── scenario-f-multi-agent.json
    │           └── scenario-g-benign-workflow.json
    │
    └── test/
        └── java/com/aios/authz/
            ├── domain/ClassificationTest.java
            ├── provenance/ProvenanceGraphTest.java
            ├── state/StateTransitionTest.java
            ├── policy/rules/…PolicyTest.java
            ├── engine/RbacBaselineEngineTest.java
            ├── engine/TrajectoryAwareEngineTest.java
            ├── api/AuthorizationControllerTest.java
            └── harness/
                ├── ScenarioComparisonTest.java   # ◀── the experiment
                └── EngineInvariantTest.java      # trajectory never ⊃ RBAC
```

### Layering rule

```text
api ──▶ engine ──▶ policy ──▶ state ──▶ provenance ──▶ domain
harness ──▶ engine
```

Dependencies point one way only. `domain` imports nothing from the project and nothing from
Spring. Enforced by an ArchUnit test (story ENG-13) rather than convention, because this is
what keeps the policy layer swappable for OPA/Cedar later.

---

## 4. API surface

Minimal by design — three endpoints, no UI.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/workflows` | Open a workflow: principal + intent. Returns `workflowId`. |
| `POST` | `/authorize` | Authorize one proposed action against workflow state. |
| `GET` | `/workflows/{id}` | Inspect trajectory, provenance and decision log. |
| `POST` | `/workflows/{id}/delegate` | Record a delegation to another principal (Sprint 4). |

`GET /workflows/{id}` exists for debugging and for the 1H attack work, not for a UI.

### Example

```http
POST /authorize
Content-Type: application/json

{
  "workflowId": "wf-7c2a",
  "principal":  { "id": "agent-42", "type": "AGENT" },
  "intent":     { "id": "partner-report" },
  "requestedAction": {
    "type": "SEND_EXTERNAL",
    "resource": "report-123",
    "destination": { "id": "partner.com", "kind": "EMAIL", "trustZone": "EXTERNAL" },
    "inputDataIds": ["report-123"]
  }
}
```

```json
{
  "decision": "DENY",
  "explanation": "Denied SEND_EXTERNAL to partner.com (EXTERNAL). report-123 derives from pricing-strategy (RESTRICTED) via GENERATE ← AGGREGATE ← READ pricing-db at step 2.",
  "evaluations": [
    { "policyId": "rbac-precheck",         "decision": "ALLOW", "reason": "agent-42 holds SEND_EXTERNAL" },
    { "policyId": "provenance-boundary",   "decision": "DENY",
      "reason": "effective classification RESTRICTED exceeds maximum PUBLIC for trust zone EXTERNAL",
      "contributingDataIds": ["pricing-strategy", "customer-42", "report-123"],
      "contributingActionIds": ["act-2", "act-5"] },
    { "policyId": "risk-budget",           "decision": "ALLOW", "reason": "12 of 100 remaining" }
  ],
  "evaluationTimeMs": 3
}
```

The explanation is **generated from the evaluation evidence**, never a hardcoded string.
An explanation that cannot be derived from `contributingDataIds` and
`contributingActionIds` is a bug — this is what makes DoD item "decision explanations are
produced" meaningful rather than cosmetic.

---

## 5. What is deliberately absent

Per [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) §10 and §36: no dashboard, no chatbot, no
LLM, no OPA/Cedar, no Kubernetes, no SSO, no message broker, no observability platform. If a
story seems to need one of these, that is a signal the story has drifted out of 1G scope.
