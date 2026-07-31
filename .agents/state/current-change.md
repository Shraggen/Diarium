# Current change

Outcome: Security scans report vulnerabilities in dependencies that can ship in the Android release application, rather than Gradle build and test tooling.
Owning bounded context: Android runtime and repository security delivery
Owning team: Shraggen
Current slice: Completed — generate the Android release runtime SBOM and scan it with Trivy in SBOM mode.
Acceptance criteria: Met — CycloneDX 3.2.4 generated 165 components from releaseRuntimeClasspath at the deterministic path; tooling and test dependencies, Netty, and Bouncy Castle are absent; representative runtime dependencies are present; CodeQL remains intact; manual and scheduled runs bypass the path filter.
Files changed: .github/workflows/security.yml, app/androidApp/build.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, .agents/state/current-change.md
Open risks: Trivy documents that third-party CycloneDX producers may omit Trivy-specific properties, although the generated Maven package URLs and versions are sufficient for standard Java vulnerability matching.
Required checks: See `.agents/config.env`
Rollout strategy: The SBOM task remains non-default; the existing security workflow invokes it on qualifying runs.
Rollback strategy: Revert the SBOM plugin/task configuration and security workflow changes; dependency locking and committed lockfiles remain unchanged.
Last updated: 2026-07-31
