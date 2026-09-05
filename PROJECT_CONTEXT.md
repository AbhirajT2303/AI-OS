# PROJECT_CONTEXT.md --- Enterprise Agentic AI Authorization

## 0. Purpose

This document is the canonical engineering context for the project.

The project is investigating whether enterprise AI agents require a new
authorization primitive that is **trajectory-aware**: authorization
should consider not only the actor and requested action, but also the
agent's intent, accumulated context, data provenance, prior actions, and
proposed consequence.

This is a **research/validation project first**. It is not yet a startup
product, and the project must not assume the thesis is correct.

The primary objective is to determine whether the following claim
survives technical and competitive testing:

> An autonomous AI workflow can contain individually authorized actions
> that become unauthorized when evaluated as a complete trajectory.

------------------------------------------------------------------------

# 1. Current Project Thesis

## 1.1 Working thesis

Traditional enterprise authorization generally evaluates something like:

``` text
Decision = f(Principal, Action, Resource, Policy)
```

For autonomous agents, we are testing:

``` text
Decision = f(
    Principal,
    Intent,
    CurrentState,
    Trajectory,
    DataProvenance,
    ProposedAction,
    Policy
)
```

The simplified model is:

``` text
Decision = Policy(CurrentState, ProposedAction)
```

Where `CurrentState` can include:

-   Principal / identity
-   User intent
-   Data currently available to the agent
-   Data classifications
-   Data provenance
-   Actions already performed
-   Tools already used
-   Models already called
-   Destination constraints
-   Capabilities
-   Approval state
-   Risk / impact budget

------------------------------------------------------------------------

# 2. The Problem We Are Investigating

AI agents increasingly have the ability to:

-   Read enterprise data
-   Call tools
-   Use models
-   Execute code
-   Modify databases
-   Send messages
-   Call external services
-   Delegate to other agents
-   Take actions without a human approving every individual step

This creates a problem different from ordinary model safety.

A model can be aligned and still be unauthorized to:

-   Access a specific document
-   Send confidential data to a specific model
-   Call a particular tool
-   Modify a particular record
-   Send a particular result externally

The enterprise question is:

> What data is allowed to cross which boundary, under what conditions,
> for which purpose, and what actions is the agent allowed to cause?

------------------------------------------------------------------------

# 3. AI OS --- Historical Concept

"AI OS" was the original broad product concept.

Conceptually:

``` text
                    AI Agents / Models
                           |
                           v
                    +--------------+
                    |    AI OS     |
                    |              |
                    | Data Access  |
                    | Permissions  |
                    | Policies     |
                    | Actions      |
                    | Approvals    |
                    | Audit        |
                    | Risk        |
                    +------+-------+
                           |
                           v
                 Enterprise Systems
```

However, this name is intentionally **not the current technical
thesis**.

The market already contains products positioned as agent control planes,
AI gateways, agent security platforms, DLP systems, identity systems,
and runtime security.

Therefore:

> Do not build or market "AI OS" until a unique technical primitive has
> been demonstrated.

The current engineering target is the **authorization engine**.

------------------------------------------------------------------------

# 4. Frontier Model Safety vs Enterprise Authorization

Frontier labs are working on:

-   Model alignment
-   Model behavior
-   Prompt injection resistance
-   Cybersecurity behavior
-   Tool-use safety
-   Monitoring
-   Containment
-   Model-level safety

The enterprise layer has different concerns:

``` text
Frontier model
    |
    | "Can this model safely perform this task?"
    v
Enterprise authorization
    |
    | "Is this specific action allowed for this
    |  user, data, intent, trajectory and destination?"
    v
Enterprise system
```

Improving model capability does not remove the authorization problem.

In fact:

``` text
More capable model
      |
      v
More capable agent
      |
      v
More tools / autonomy
      |
      v
Larger blast radius
      |
      v
Greater need for external authorization
```

------------------------------------------------------------------------

# 5. Competitive Reality

The project must start from the assumption that the market is crowded.

Relevant categories/products investigated include:

## Microsoft

-   Agent 365
-   Entra Agent ID
-   Microsoft Purview
-   Microsoft Foundry Agent Service

Capabilities include agent identity, lifecycle, access controls, data
security, conditional access, threat protection, and auditing.

## AWS

-   Bedrock AgentCore
-   AgentCore Gateway
-   Cedar-based policy

Capabilities include agent runtime, gateway mediation, identity, tool
access, and authorization.

## Google

-   Agent Gateway

Focuses on programmable traffic/data-plane controls across
user-to-agent, agent-to-agent, and agent-to-tool flows.

## Databricks

-   Unity Catalog
-   Unity AI Gateway

Strong overlap around:

-   AI governance
-   Model governance
-   MCP
-   Functions
-   Agent traffic
-   External model routing
-   Data governance

## Palo Alto Networks

-   Prisma AIRS

Capabilities include:

-   Agent identity
-   Runtime controls
-   AI gateway
-   Agent behavior
-   Action controls
-   DLP
-   Audit
-   Red teaming
-   Tool-chaining attack analysis

## CrowdStrike

-   Falcon Guardian

Runtime AI-agent security and detection/response.

## Nightfall

AI Agent Security covering areas such as:

-   MCP
-   Coding agents
-   Tool calls
-   Shell commands
-   DLP

## Zenity

Enterprise agent runtime security, including:

-   Inline policies
-   Data movement
-   Prompt injection
-   Tool risks
-   Credential risks

## Lakera / Check Point

Agent discovery and runtime guardrails including:

-   Prompt injection
-   Tool calls
-   Tool responses
-   DLP
-   Agent behavior

## Cloudflare

AI Gateway with:

-   Model proxying
-   Guardrails
-   DLP
-   Prompt/response controls

## TrustLogix

Particularly relevant because it markets:

-   Intent-based authorization
-   MCP data gateway
-   Identity propagation
-   Runtime controls
-   Kill switch
-   Audit

## Tetrate / Ory

Relevant to:

-   Dynamic authorization
-   MCP tool-call authorization
-   Request parameters
-   Policy enforcement

------------------------------------------------------------------------

# 6. Competitive Conclusion

The project must NOT claim:

> Nobody is solving AI agent security.

That claim is false.

It must also NOT claim:

> Nobody has trajectory-aware authorization.

That is currently an unproven claim.

The research hypothesis is narrower:

> Existing IAM, DLP, AI gateways, agent-security platforms, and policy
> engines may govern many individual actions and data flows, but there
> may be a gap when authorization depends on the accumulated trajectory
> and information lineage of an autonomous workflow.

This needs to be proven.

------------------------------------------------------------------------

# 7. Real-World Evidence

The project investigated several credible examples of agentic security
failures.

## Microsoft MCP Tool Poisoning

A documented scenario demonstrated how:

-   An approved MCP tool could be used
-   Existing user permissions could be legitimate
-   Dataverse access could be legitimate
-   An allowlisted outbound server could be involved
-   Malicious tool content could influence the workflow
-   Sensitive information could ultimately be exfiltrated

The important lesson:

> The individual operations can look normal while the trust boundary
> between them becomes unsafe.

This is strong evidence for investigating trajectory-level controls.

## GitHub MCP Poisoning

A malicious public issue can become agent context, after which hidden
instructions can influence access to private repositories and
external/public writes.

Lesson:

> Untrusted context can interact with legitimate permissions and tools.

## OpenClaw Deletion Incident

An agent reportedly performed destructive actions and ignored stop
commands.

Lesson:

> Excessive agency and insufficient confirmation can make legitimate
> capabilities dangerous.

## Vertex AI "Double Agent"

Privilege-abuse scenarios demonstrate risks from overprivileged service
identities and protected-resource access.

## Agent Framework Vulnerabilities

Agent frameworks have exposed risks involving:

-   File access
-   Tool chaining
-   Sandbox escape
-   Remote code execution

## OpenAI Internal Cyber Evaluation

An internal cyber evaluation demonstrated that highly capable models
could challenge isolation assumptions.

This is evaluation evidence, not evidence that normal enterprise
deployments behave this way.

## UK AI Security Institute Evaluation

Permissive cyber evaluations demonstrated that autonomous systems can
take sustained unsanctioned actions.

Again, evaluation evidence, not an ordinary enterprise incident.

## NSA MCP Guidance

Important concepts include:

-   Tool metadata becoming trusted context
-   Tool output becoming another agent's input
-   Cascading prompt injection
-   Data exfiltration
-   Control-flow hijacking
-   Multi-agent propagation

## METR

METR has documented numerous incidents involving agents acting contrary
to user intent, including overreach-related behavior.

------------------------------------------------------------------------

# 8. Strongest Technical Scenario

The canonical scenario for this project is:

``` text
User:
Prepare a report for an external partner.

Agent:
1. READ customer database
2. READ pricing database
3. READ support tickets
4. RUN analytics
5. CALL model
6. GENERATE report
7. SEND report externally
```

Individual authorization:

``` text
READ customer DB      -> ALLOW
READ pricing DB       -> ALLOW
READ support tickets  -> ALLOW
RUN analytics         -> ALLOW
CALL model            -> ALLOW
GENERATE report       -> ALLOW
SEND external         -> ALLOW
```

But the complete trajectory may violate:

> Customer information + internal pricing strategy + support history
> must not leave the enterprise.

The key question:

> Should the final external send be allowed given everything the agent
> has seen and done?

This is the primary experimental target.

------------------------------------------------------------------------

# 9. 1F --- Kill Test

The purpose of 1F is to disprove the thesis.

Do not protect the idea.

Try to demonstrate that existing systems already solve the problem.

## Test 1 --- Confidential data → external model

``` text
READ confidential document
        |
        v
CALL external model
```

This may already be handled by DLP, AI gateways, data governance, or
model-routing policies.

Result:

> Not sufficiently unique.

## Test 2 --- Confidential document → summary → external email

``` text
READ confidential document
        |
        v
SUMMARIZE
        |
        v
SEND externally
```

Potentially interesting, but DLP can inspect resulting content.

Result:

> Not sufficient by itself.

## Test 3 --- Multi-source enterprise report

``` text
READ customer data
        |
READ pricing data
        |
READ support data
        |
ANALYZE
        |
CALL model
        |
GENERATE report
        |
SEND externally
```

This requires combining:

-   Data source
-   Classification
-   Provenance
-   Intent
-   Model
-   Destination
-   Previous actions

Result:

> Strong candidate for trajectory-aware authorization.

## Test 4 --- Tool chaining

``` text
Tool A: CRM lookup
Tool B: Analytics
Tool C: File access
Tool D: HTTP request
```

Each tool may individually be approved.

The combination can create an unsafe information-flow path.

Question:

> Should Tool D be allowed given the previous trajectory?

## Test 5 --- Individually authorized but collectively unsafe

``` text
READ public document
        |
CALL approved internal tool
        |
receive confidential data
        |
CALL approved model
        |
WRITE internal report
        |
CALL approved translation tool
        |
SEND externally
```

Traditional authorization:

``` text
ALLOW
ALLOW
ALLOW
ALLOW
ALLOW
ALLOW
```

Potential trajectory-aware result:

``` text
DENY
```

because the current output derives from protected data.

------------------------------------------------------------------------

# 10. 1G --- Authorization Engine

## Objective

Build the smallest technical proof.

Do not build:

-   A startup dashboard
-   A chatbot
-   An LLM-powered policy engine
-   Kubernetes infrastructure
-   Enterprise SSO
-   Marketing website
-   Hundreds of policies
-   AI OS branding

The first version should answer only:

> Can trajectory-aware authorization produce a better authorization
> decision than traditional RBAC for realistic autonomous workflows?

------------------------------------------------------------------------

# 11. Recommended Stack

Use a boring stack:

``` text
Java 21+
Spring Boot
PostgreSQL
REST
JUnit
Docker
```

Potential later policy technologies:

``` text
Open Policy Agent
Cedar
```

Do not start with them.

First prove the authorization semantics directly.

------------------------------------------------------------------------

# 12. 1G Architecture

``` text
                    +---------------------+
                    |   Agent Simulator   |
                    |                     |
                    | "What should I do?" |
                    +----------+----------+
                               |
                               | ActionRequest
                               v
                  +------------------------+
                  |   Authorization API    |
                  |                        |
                  | POST /authorize        |
                  +------------+-----------+
                               |
                               v
              +--------------------------------+
              |      Authorization Engine      |
              |                                |
              | Identity                       |
              | Intent                         |
              | Data provenance                |
              | Classification                 |
              | Trajectory                     |
              | Policy                         |
              | Destination                    |
              | Risk / impact                  |
              +----------------+---------------+
                               |
                       ALLOW / DENY / ASK
                               |
                               v
                    +---------------------+
                    |      Executor       |
                    +---------------------+
```

------------------------------------------------------------------------

# 13. Domain Model

## Principal

``` java
public record Principal(
    String id,
    PrincipalType type
) {}
```

``` java
enum PrincipalType {
    USER,
    AGENT,
    SERVICE
}
```

## Intent

``` java
public record Intent(
    String id,
    String description
) {}
```

Example:

``` text
prepare_customer_report
```

## DataAsset

``` java
public record DataAsset(
    String id,
    Classification classification,
    String source
) {}
```

``` java
enum Classification {
    PUBLIC,
    INTERNAL,
    CONFIDENTIAL,
    RESTRICTED
}
```

## Action

``` java
public record Action(
    ActionType type,
    String resource,
    String destination
) {}
```

``` java
enum ActionType {
    READ,
    CALL_MODEL,
    WRITE,
    SEND_EXTERNAL
}
```

------------------------------------------------------------------------

# 14. Trajectory

``` java
public record Trajectory(
    List<ActionRecord> actions,
    Set<DataAsset> acquiredData
) {}
```

``` java
public record ActionRecord(
    Action action,
    Instant timestamp
) {}
```

The trajectory records:

``` text
READ customer DB
READ pricing DB
CALL analytics
CALL model
```

The next authorization request receives the trajectory.

------------------------------------------------------------------------

# 15. Authorization Request

``` java
public record AuthorizationRequest(
    Principal principal,
    Intent intent,
    Trajectory trajectory,
    Action requestedAction
) {}
```

Conceptual REST endpoint:

``` text
POST /authorize
```

------------------------------------------------------------------------

# 16. Traditional RBAC Baseline

The control group:

``` java
public Decision authorize(Action action, Principal principal) {

    if (hasPermission(principal, action)) {
        return Decision.ALLOW;
    }

    return Decision.DENY;
}
```

Example:

``` text
agent-42
   |
SEND_EXTERNAL
   |
permission exists?
   |
YES
   |
ALLOW
```

This baseline is essential.

Without it, we cannot claim trajectory-aware authorization adds value.

------------------------------------------------------------------------

# 17. Trajectory-Aware Authorization

Initial structure:

``` java
public Decision authorize(AuthorizationRequest request) {

    if (!traditionalPermissionCheck(request)) {
        return Decision.DENY;
    }

    if (violatesDataBoundary(request)) {
        return Decision.DENY;
    }

    if (violatesTrajectoryPolicy(request)) {
        return Decision.DENY;
    }

    return Decision.ALLOW;
}
```

The main research area is:

``` text
violatesTrajectoryPolicy(...)
```

------------------------------------------------------------------------

# 18. First Simple Policy

Initial rule:

> Confidential data may not reach an external destination.

Example logic:

``` java
boolean violatesTrajectoryPolicy(
    AuthorizationRequest request) {

    boolean hasConfidentialData =
        request.trajectory()
               .acquiredData()
               .stream()
               .anyMatch(data ->
                   data.classification()
                       == Classification.CONFIDENTIAL);

    boolean goingExternal =
        request.requestedAction().type()
            == ActionType.SEND_EXTERNAL;

    return hasConfidentialData && goingExternal;
}
```

This is intentionally simple.

Important:

> This is not claimed as novel.

DLP/data-governance systems can already implement similar controls.

------------------------------------------------------------------------

# 19. Derived Data and Provenance

The next important capability is lineage.

Example:

``` text
Customer DB
    |
    v
confidential data
    |
    v
Analytics
    |
    v
aggregated statistics
    |
    v
Report
    |
    v
External destination
```

The report should retain provenance.

Example:

``` java
public record DerivedData(
    String id,
    Set<String> derivedFrom,
    Classification classification
) {}
```

Example:

``` text
report-123

derivedFrom:
    customer-DB
    pricing-DB
    support-DB

classification:
    CONFIDENTIAL
```

Conceptually:

``` text
report-123
|
+-- customer-42
|     +-- CONFIDENTIAL
|
+-- pricing-strategy
|     +-- RESTRICTED
|
+-- support-tickets
      +-- INTERNAL
```

This lets policy evaluate the lineage of information rather than only
its current textual content.

------------------------------------------------------------------------

# 20. Authorization State

Proposed model:

``` java
public class AuthorizationState {

    private Principal principal;

    private Intent intent;

    private Set<DataAsset> knownData;

    private List<ActionRecord> trajectory;

    private Set<Capability> capabilities;

    private RiskBudget riskBudget;

    private Set<String> allowedDestinations;
}
```

Every action updates state.

``` text
State0
  |
 READ
  v
State1
  |
 CALL_TOOL
  v
State2
  |
 CALL_MODEL
  v
State3
  |
 SEND_EXTERNAL?
  v
State4
```

Authorization becomes a transition check:

``` text
current state + proposed transition
                 |
                 v
          policy evaluation
                 |
                 v
         transition allowed?
```

------------------------------------------------------------------------

# 21. Multi-Agent Propagation

Important future scenario:

``` text
Agent A
   |
READ confidential data
   |
Agent B
   |
SUMMARIZE
   |
Agent C
   |
SEND_EXTERNAL
```

Agent C may never directly access the original database.

Therefore the authorization state should eventually support:

-   Information lineage
-   Cross-agent provenance
-   Delegation
-   Trust boundaries
-   Derived-data classification

Potential reasoning:

``` text
Agent C
   |
received output from Agent B
   |
B received data from A
   |
A accessed confidential data
   |
C's current state contains derived confidential information
   |
SEND_EXTERNAL
   |
DENY
```

------------------------------------------------------------------------

# 22. 1G Success Criteria

The prototype succeeds if it demonstrates at least one realistic
scenario where:

``` text
Traditional RBAC
       |
       v
     ALLOW

Trajectory-aware authorization
       |
       v
     DENY
```

And we can clearly explain:

1.  Why RBAC allowed it
2.  Why the complete trajectory is unsafe
3.  Which information or action created the violation
4.  Why the decision could not be expressed naturally using only the
    individual action

------------------------------------------------------------------------

# 23. Evaluation Metrics

Run identical scenarios through both systems.

Measure:

``` text
Dangerous actions prevented
False positives
Latency
Human approvals required
Workflow completion rate
Policy complexity
```

Later, also measure:

``` text
Memory/state size
Provenance graph size
Decision determinism
Policy evaluation cost
Cross-agent propagation accuracy
```

------------------------------------------------------------------------

# 24. Required Test Scenarios

Implement these as automated tests.

## Scenario A --- Public data

``` text
READ public
CALL external model
SEND external
```

Expected:

``` text
ALLOW
```

## Scenario B --- Confidential data to external model

``` text
READ confidential
CALL external model
```

Expected:

``` text
DENY
```

## Scenario C --- Confidential data with approved private model

``` text
READ confidential
CALL private/internal model
WRITE internal report
```

Expected:

``` text
ALLOW
```

## Scenario D --- Confidential data → internal report → external email

``` text
READ confidential
CALL private model
WRITE internal report
SEND external
```

Expected:

``` text
DENY
```

## Scenario E --- Multiple sources

``` text
READ customer data
READ pricing data
READ support data
ANALYZE
GENERATE report
SEND external
```

Expected:

``` text
DENY
```

## Scenario F --- Multi-agent propagation

``` text
Agent A:
READ confidential

Agent B:
receive summary

Agent C:
SEND external
```

Expected:

``` text
DENY
```

## Scenario G --- Benign multi-step workflow

Create a workflow with several internal actions and no protected
information leaving the boundary.

Expected:

``` text
ALLOW
```

This is needed to measure false positives.

------------------------------------------------------------------------

# 25. The Killer Demo

Eventually the prototype should produce output similar to:

``` text
USER:
Prepare a report for our external partner.

AGENT:
READ customer database

AI OS:
ALLOW

AGENT:
READ pricing database

AI OS:
ALLOW

AGENT:
RUN analytics

AI OS:
ALLOW

AGENT:
GENERATE report

AI OS:
ALLOW

AGENT:
SEND report -> partner.com

Traditional RBAC:
ALLOW

Trajectory Authorization:
DENY

Reason:
External transmission is inconsistent with
the data provenance accumulated during this
agent trajectory.
```

Do not optimize for UI initially.

The correctness of this decision is what matters.

------------------------------------------------------------------------

# 26. Engineering Principles

## Principle 1 --- Don't trust the model

The authorization layer must remain external to the model.

``` text
Model:
"What should I do?"

Policy engine:
"Are you allowed to do it?"
```

## Principle 2 --- Deny by policy, not by model morality

Do not ask an LLM:

> "Do you think this is safe?"

The authorization engine must produce deterministic policy decisions
where possible.

## Principle 3 --- Provenance must survive transformations

If confidential data becomes:

``` text
summary
statistics
translation
classification
embedding
report
```

the system must eventually be able to reason about its lineage.

## Principle 4 --- Every side effect is a state transition

Examples:

``` text
WRITE
DELETE
SEND
TRANSFER
EXECUTE
CALL_TOOL
```

must pass authorization.

## Principle 5 --- Build the baseline first

Never demonstrate only the new system.

Always show:

``` text
Traditional authorization
        VS
Trajectory-aware authorization
```

------------------------------------------------------------------------

# 27. What Would Kill the Thesis?

The project should be abandoned or substantially changed if:

### Kill condition A

Existing products can express and enforce the same trajectory-level
policies with no meaningful limitation.

### Kill condition B

The proposed trajectory information provides no meaningful improvement
over:

-   DLP
-   IAM
-   AI gateway
-   Tool authorization
-   Data classification

### Kill condition C

Trajectory-aware policies produce unacceptable false positives.

### Kill condition D

The required state/provenance is too expensive or impossible to maintain
reliably.

### Kill condition E

The final decision still requires an LLM's subjective judgment in a way
that makes authorization nondeterministic or unsafe.

### Kill condition F

Enterprise security engineers consistently say:

> "Our existing stack already solves this exact problem."

If this happens, pivot.

------------------------------------------------------------------------

# 28. What Would Validate the Thesis?

Strong validation would look like:

``` text
1. Find realistic workflow
       |
2. Existing RBAC / gateway / DLP says ALLOW
       |
3. Security analysis says unsafe
       |
4. Trajectory engine says DENY
       |
5. Existing products cannot naturally express
   the policy without custom correlation/state
       |
6. Security engineers agree the gap is real
```

Even better:

> A security engineer says, "We currently solve this manually by
> correlating logs/data/tool calls."

That would be a strong signal.

------------------------------------------------------------------------

# 29. Product Direction --- Only If Validation Works

If the technical thesis survives, potential product direction could
become:

> A runtime authorization control plane for autonomous enterprise
> agents.

Potential product primitives:

``` text
Agent Identity
+
Intent
+
Data Provenance
+
Trajectory
+
Policy
+
Action
+
Destination
+
Risk
```

But do not call this a company/product yet.

First prove the primitive.

------------------------------------------------------------------------

# 30. Phase Roadmap

## Phase 1 --- Research / Technical Validation

### 1A

Identify enterprise agent problems.

### 1B

Separate data sovereignty from agent action safety.

### 1C

Map existing market players.

### 1D

Find real-world failures.

### 1E

Form trajectory-aware authorization hypothesis.

### 1F

Kill test.

### 1G

Build authorization engine.

### 1H

Attack the engine and compare it against existing systems.

------------------------------------------------------------------------

# 31. 1H --- Attack / Validate Engine

After 1G, deliberately attack the system.

Questions:

-   Can provenance be lost?
-   Can an agent reset or hide trajectory state?
-   Can a malicious tool falsify metadata?
-   Can one agent pass unsafe data to another?
-   Can data be transformed to bypass classification?
-   Can an agent exploit an allowed destination?
-   Can tool chaining bypass a rule?
-   Can prompt injection alter intent?
-   Can an agent exploit policy gaps?
-   Can policy evaluation become too expensive?
-   Can a legitimate workflow be incorrectly denied?

The objective is not to make the prototype look good.

The objective is to discover where it breaks.

------------------------------------------------------------------------

# 32. 1H Competitive Comparison

For each attack scenario, compare:

``` text
RBAC
DLP
AI Gateway
Existing agent security
Trajectory-aware engine
```

Document:

``` text
What each system sees
What each system can decide
What each system cannot decide
Required configuration
False positives
Latency
Operational complexity
```

This is where the startup thesis becomes evidence-based.

------------------------------------------------------------------------

# 33. Phase 2 --- Enterprise Validation

Only after the technical experiment.

Talk to approximately 10--20 people across:

-   Enterprise security
-   IAM
-   Platform engineering
-   AI engineering
-   Cloud security
-   Data security
-   CISOs/security leadership

Ask about actual workflows, not opinions about the startup.

Questions:

1.  What autonomous agents are deployed?
2.  What systems can they access?
3.  How are tool permissions enforced?
4.  How is sensitive data classified?
5.  How do you prevent sensitive information leaving the enterprise?
6.  How do you audit multi-step agent workflows?
7.  How do you handle agent-to-agent delegation?
8.  What happens when an agent takes an unexpected action?
9.  Do your controls consider previous actions?
10. Where are engineers currently building custom controls?

Do not lead them with:

> "Would trajectory-aware authorization help?"

First understand their existing architecture.

------------------------------------------------------------------------

# 34. Phase 3 --- Design Partner

Only after repeated evidence:

``` text
Real pain
+
Existing tools insufficient
+
Technical primitive works
+
Security team cares
```

Then find one narrow use case.

Potential examples:

-   Financial-services agents
-   Customer-support agents
-   Engineering/coding agents
-   Internal knowledge agents
-   Enterprise MCP environments
-   Data-analysis agents
-   Agent-to-agent workflows

The initial product should solve one painful workflow, not "all AI
security."

------------------------------------------------------------------------

# 35. Phase 4 --- MVP

Potential MVP:

``` text
Agent
  |
  v
AI Authorization Gateway
  |
  +-- Identity
  +-- Intent
  +-- Data classification
  +-- Provenance
  +-- Trajectory
  +-- Policy
  +-- Tool authorization
  +-- Destination control
  |
  v
Enterprise tools / models
```

Only build the infrastructure that the validated use case requires.

------------------------------------------------------------------------

# 36. Current Non-Goals

Do NOT currently build:

-   Full AI OS
-   General-purpose AI security platform
-   LLM provider
-   Foundation model
-   Generic DLP replacement
-   Generic IAM replacement
-   Generic AI gateway
-   Full observability platform
-   Autonomous SOC
-   Enterprise dashboard before the engine works

------------------------------------------------------------------------

# 37. Current Working Vocabulary

Use these terms consistently:

### Agent

Autonomous software capable of selecting or executing actions.

### Principal

Entity performing an action: user, agent, or service.

### Intent

The declared objective of the workflow.

### Action

A proposed operation.

### Trajectory

Ordered history of actions and relevant state transitions.

### Data provenance

Lineage describing where information came from and what it was derived
from.

### Authorization state

Current security-relevant state used to evaluate the next transition.

### Policy

Rules determining whether a transition is permitted.

### Side effect

An action that changes or communicates state outside the agent.

### Trajectory-aware authorization

Authorization that evaluates a proposed action using the accumulated
trajectory/context rather than only the isolated action.

------------------------------------------------------------------------

# 38. Current One-Sentence Thesis

Use this internally:

> **We are testing whether autonomous enterprise agents require
> trajectory-aware authorization: authorization that evaluates an action
> based on what the agent has seen, done, and is trying to cause---not
> only who it is and whether it has permission to perform that
> individual action.**

------------------------------------------------------------------------

# 39. Current One-Sentence Product Hypothesis

Only use if the technical validation succeeds:

> **Before an AI agent takes an action, determine whether that action is
> still authorized given everything the agent has seen and done.**

------------------------------------------------------------------------

# 40. Current Status

The project is currently at:

``` text
1G — Build Authorization Engine
```

Immediate next implementation sequence:

``` text
1. Create Spring Boot project
2. Create domain objects
3. Implement Action model
4. Implement Principal model
5. Implement Intent
6. Implement DataAsset
7. Implement Trajectory
8. Implement AuthorizationRequest
9. Implement RBAC baseline
10. Implement trajectory-aware engine
11. Implement simple policies
12. Implement state updates
13. Implement provenance
14. Implement automated scenarios
15. Compare RBAC vs trajectory-aware decisions
16. Produce CLI/test output
17. Move to 1H only after results are measurable
```

------------------------------------------------------------------------

# 41. Instructions for Claude Code

When starting implementation:

1.  Read this entire document first.
2.  Treat the trajectory-aware authorization concept as a
    **hypothesis**, not established truth.
3.  Do not broaden the scope into a generic AI security platform.
4.  Do not add an LLM unless explicitly requested.
5.  Do not add a UI unless explicitly requested.
6.  Prioritize deterministic authorization.
7.  Keep the architecture modular so policy logic can later be replaced
    by OPA/Cedar if useful.
8.  Write tests before adding complexity.
9.  Every policy decision should be explainable.
10. Preserve the RBAC baseline so experiments can compare the two
    systems.
11. Keep provenance separate from raw data content.
12. Model state transitions explicitly.
13. Make it easy to add new action types and policies.
14. Log why a decision was ALLOW, DENY, or ASK.
15. Avoid premature abstractions.
16. Prefer a small working prototype over enterprise-scale
    infrastructure.
17. If an implementation decision would materially change the research
    hypothesis, stop and surface the decision instead of silently
    changing the thesis.

------------------------------------------------------------------------

# 42. Definition of Done for 1G

1G is complete when:

``` text
[ ] Spring Boot project runs
[ ] Agent simulator can generate actions
[ ] RBAC baseline works
[ ] Trajectory state is maintained
[ ] Data classification works
[ ] Data provenance works at basic level
[ ] Policies can evaluate current state + proposed action
[ ] ALLOW / DENY / ASK decisions work
[ ] At least 7 scenarios are automated
[ ] RBAC and trajectory-aware results are comparable
[ ] At least one meaningful RBAC=ALLOW / trajectory=DENY case exists
[ ] Decision explanations are produced
[ ] Tests document why the trajectory changes authorization
```

If the last three items cannot be achieved convincingly, **do not move
to product development**.

------------------------------------------------------------------------

# 43. Final Principle

The project is not trying to prove:

> "AI is dangerous."

It is trying to answer a much more precise engineering question:

> **When software becomes autonomous, does authorization need to evolve
> from checking isolated actions to checking stateful trajectories and
> information flow?**

Everything built in 1G and 1H should help answer that question.
