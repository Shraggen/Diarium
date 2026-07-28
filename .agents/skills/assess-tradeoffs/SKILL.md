---
name: assess-tradeoffs
description: Analyze a refactor, migration, boundary change, data ownership change, or infrastructure-affecting decision before implementation. Use when work can alter coupling, bounded contexts, deployment risk, architectural characteristics, or team coordination.
compatibility: Agent Skills-compatible clients with repository read access. No network required.
metadata:
  owner: architecture
  version: "1.0.0"
  maturity: starter
---

# Assess trade-offs

## Purpose

Produce a grounded architecture decision before code is changed. Do not edit code
while this skill is active.

## Trigger conditions

Use this skill when the request:

- crosses a bounded context or team ownership line
- changes a public API, schema, event, queue, or data owner
- proposes extraction, splitting, merging, replatforming, or a shared library
- changes security, resilience, consistency, latency, scalability, operability,
  deployability, or recovery characteristics
- touches a central legacy subsystem or requires several coordinated deployments

## Evidence to collect

Collect the smallest useful evidence set:

- business or operator outcome
- owning bounded context and team
- affected user journey
- current paths, dependencies, contracts, and data ownership
- incidents, churn, slow checks, coupling, or deployment friction
- architecture characteristics that are load-bearing
- observability and rollback options
- uncertainty and missing evidence

State assumptions explicitly rather than blocking indefinitely on missing information.

## Workflow

1. Restate the request in ubiquitous language.
2. Describe the current-state forces and the cost of doing nothing.
3. Produce at least three viable options, including a minimal-change/status-quo option.
4. Evaluate each option qualitatively unless measured data justifies numbers.
5. Recommend the smallest reversible move that plausibly satisfies the outcome.
6. Identify evidence that would change the recommendation.
7. Define the first trunk-safe slice and mandatory guardrails.

## Required evaluation dimensions

- simplicity
- delivery speed and batch size
- deployability
- reversibility and recovery speed
- change failure risk
- static, runtime, and communication coupling
- data consistency and ownership
- testability and operability
- performance and scalability
- security and compliance exposure
- team cognitive load
- bounded-context integrity

For each option, state the likely direction for deployment frequency, lead time,
change failure rate, and recovery time: `improves`, `neutral`, `worsens`, or `unknown`.

## Guardrails

- Do not recommend a large rewrite when a reversible thin slice exists.
- Do not move domain logic to a shared utility merely to remove duplication.
- Do not propose a service or package without an owner, contract, deploy path,
  observability plan, and rollback posture.
- Do not hide uncertainty behind fake numeric precision.

## Output

Return exactly:

1. Architecture decision summary
2. Current-state forces
3. Option matrix
4. DORA and operability implications
5. Boundary, data, and coupling implications
6. Recommendation and why alternatives lost
7. Evidence that would change the decision
8. First safe slice
9. Required guardrails
10. Rollback posture
11. ADR-lite: Context, Decision, Consequences, Deferred decisions

## Completion criteria

- at least three options were considered
- DORA, team, and boundary effects are explicit
- the recommendation is reversible or explains why not
- a first safe slice and rollback path exist
- no code has been changed
