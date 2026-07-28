---
name: trunk-delivery
description: Turn a feature, bug fix, or migration into acceptance-first, independently mergeable, trunk-safe slices with narrow verification and explicit rollback. Use for normal delivery that does not require a new architecture decision.
compatibility: Agent Skills-compatible clients with repository write access and local build or test tools.
metadata:
  owner: delivery-engineering
  version: "1.0.0"
  maturity: starter
---

# Trunk delivery

## Purpose

Deliver the smallest safe user or operator outcome while keeping the repository
releasable after each completed slice.

## Before implementation

Determine:

- requirement and acceptance criteria
- owning bounded context
- current behavior and relevant tests
- delivery, rollout, and rollback strategy
- whether the path is legacy or high risk
- the narrowest useful verification commands

If the request changes architecture, ownership, contracts, or load-bearing
characteristics, invoke `assess-tradeoffs` first.

## Workflow

1. Write executable behavioral acceptance criteria.
2. Divide work into atomic, independently reviewable slices.
3. For incomplete behavior, choose a feature flag, dark launch, abstraction, or
   non-default path before implementation.
4. For each slice:
   - create or update the first failing test
   - confirm the failure
   - make the minimum code change
   - run formatting, lint/static analysis, type-check/compile, focused tests,
     contract tests, and changed acceptance tests as applicable
   - record rollback and the next slice
5. Keep each commit limited to one domain outcome.

## Required slice table

For every planned slice include:

- slice and user/operator value
- trunk-safe artifact
- hidden behind or safety mechanism
- local verification
- rollout and rollback
- domain-intent commit title

## Anti-rationalization checks

Reject:

- “tests can come later”
- “the flag can come after the feature”
- “several unrelated fixes belong in this commit”
- “the whole rewrite must land before anything works”
- “a unit test is enough despite a changed public behavior”
- “we can cross the context directly and clean it up later”

## Output before coding

1. Delivery summary
2. Acceptance criteria
3. Slice plan
4. Immediate next slice and first failing test
5. Verification commands
6. Rollout and rollback plan

## Completion criteria

- acceptance criteria are executable
- slices are independently mergeable and reversible
- completed slices passed their feedback ladder
- cross-context calls use published interfaces
- trunk remains releasable if work stops now
