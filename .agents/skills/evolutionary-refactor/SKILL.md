---
name: evolutionary-refactor
description: Modernize legacy or structurally difficult code through characterization tests, seams, reversible slices, architecture checks, and gradual migration patterns. Use after trade-offs are understood and a safe target shape exists.
compatibility: Agent Skills-compatible clients with repository write access and local build or test tools.
metadata:
  owner: architecture
  version: "1.0.0"
  maturity: starter
---

# Evolutionary refactor

## Purpose

Improve structure without a flag-day rewrite or hidden behavior change. Leave the
repository deployable after every completed slice.

## Required reconnaissance

Establish:

- outcome and owning bounded context
- owners and dependent consumers
- runtime entry points
- current behavior and tests protecting it
- contracts, schemas, events, and data ownership touched
- architecture characteristics and checks
- operational blast radius
- rollout and rollback path

## Workflow

### 1. Freeze observable behavior

When tests are weak, add characterization tests at the most useful seam:

- API or command boundary
- service or module boundary
- event or serialization boundary
- critical domain rule
- persistence contract

Document ambiguous behavior and preserve it unless the request explicitly changes it.

### 2. Define the target shape

Describe:

- responsibilities and owning context
- stable published interfaces
- domain versus infrastructure placement
- characteristics that must remain protected
- smallest seam that enables gradual movement

### 3. Choose one primary migration pattern

**Branch by abstraction** — replace an internal implementation behind a stable interface.

**Expand, migrate, contract** — evolve a published API, schema, or event without breaking consumers.

**Feature-flagged replacement** — run old and new behavior safely when runtime rollback must be immediate.

### 4. Plan thin slices

Each slice contains:

- one objective
- likely files or components
- behavior change or explicit “no behavior change”
- tests and architecture checks
- rollout and rollback action

Prefer one dependency edge, seam, or responsibility movement per slice.

### 5. Execute and verify one slice

1. Restate the exact objective.
2. Create or update the failing/protective test.
3. Make the minimum change.
4. Run the nearest feedback ladder.
5. Confirm behavior, boundaries, deployability, and rollback.
6. Update `.agents/state/current-change.md`.
7. Stop if the slice has expanded beyond its original boundary.

## Stop conditions

Stop implementation and return to `assess-tradeoffs` when:

- characterization tests reveal contradictory or unclear behavior
- several consumers require a breaking contract change
- the change only works as a flag day
- blast radius is larger than assessed
- architecture or boundary failures imply the target shape is wrong
- progress requires combining multiple unverified high-risk edits
- data ownership or team ownership cannot be stated clearly

## Output

1. Legacy risk summary
2. Behavior protection plan
3. Target shape
4. Chosen migration pattern
5. Step-by-step evolutionary plan
6. Verification plan
7. Rollout and rollback plan
8. Execution log, when edits are performed
9. Final state and remaining work

## Completion criteria

- current behavior is protected or intentional changes are explicit
- every completed slice was independently verified
- architecture and team boundaries pass
- trunk remains releasable
- rollback remains practical
- permanent protection was added where useful
