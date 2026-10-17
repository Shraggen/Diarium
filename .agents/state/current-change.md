# Current change

Outcome: Contributors build without installing JDK 21, and the test suites are checked by generated properties and by mutation testing.
Owning bounded context: Build tooling and core tool-contract verification
Owning team: Shraggen
Current slice: Gradle-provisioned JDK 21 toolchain; core property tests for schema serialization, schema DSL, model tool-call parsing, tool mapping, and tool arguments; report-only Pitest `mutationTest` tasks for `core` and `sharedLogic`; fix for multi-tool prompt indentation found by the mapper property.
Acceptance criteria: CI passes `:core:jvmTest`, `:app:sharedLogic:allTests`, and the Android build on the provisioned toolchain; the non-blocking CI mutation step publishes Pitest reports for both modules.
Files changed: gradle.properties, gradle/libs.versions.toml, build.gradle.kts, core/build.gradle.kts, app/*/build.gradle.kts, core and sharedLogic lockfiles, core property tests, LlamatikToolMapper.kt, .github/workflows/ci.yml, docs/getting-started.md, docs/testing.md
Open risks: Tests were verified only in CI, not locally. Pitest mutates Kotlin bytecode without a Kotlin-aware plugin, so some survivors are compiler-generated noise. The `sharedLogic` mutation run depends on an AGP-internal runtime-classes jar path. `gradlew` still needs some Java 17+ to launch, and the Android SDK root and license acceptance are not automated.
Required checks: See `.agents/config.env`
Rollout strategy: Mutation testing is report-only (`continue-on-error`, no threshold). Promoting a mutation-score threshold to a blocking gate requires explicit approval.
Rollback strategy: Revert the commits; lockfile additions are confined to test and `pitest` configurations.
Last updated: 2026-10-17
