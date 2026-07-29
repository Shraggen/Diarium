# Current change

Outcome: Readers can learn to build Diarium, verify a change, or understand the design without crossing documentation types.
Owning bounded context: Repository-wide contributor documentation
Owning team: Shraggen
Current slice: Completed — added a build tutorial and verification how-to, then routed readers to them from the documentation entry points.
Acceptance criteria: Met — the documented build produced four ABI-specific debug APKs; contributors can choose a verification command by change scope; README and site navigation link to both pages; local documentation links resolve; strict MkDocs validation passes.
Files changed: README.md, docs/getting-started.md, docs/verifying-changes.md, docs/index.md, docs/testing.md, mkdocs.yml, .agents/state/current-change.md
Open risks: None recorded
Required checks: See `.agents/config.env`
Rollout strategy: Documentation-only change; publish through the existing documentation workflow after merge.
Rollback strategy: Revert the documentation pages and their navigation links as one independent change.
Last updated: 2026-07-29
