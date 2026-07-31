# Why source paths are deep

Diarium's source paths look repetitive because Gradle modules, Kotlin
Multiplatform source sets, source languages, JVM packages, and feature names
all express different kinds of structure. Keeping those layers visible makes
platform and dependency boundaries easier to recognise, even though it adds
directory depth.

The directory depth in this repository comes from several independent naming
systems being written into one path. For example:

```text
app/sharedLogic/src/commonMain/kotlin/com/shraggen/diarium/speech/Transcript.kt
│       │       │    │        │              │
module  Gradle  KMP  language package        feature
```

## Each layer answers a different question

- `app/sharedLogic` identifies the Gradle module and its dependency boundary.
- `src/commonMain` is the conventional Kotlin Multiplatform source-set layout.
  Gradle can be reconfigured, but keeping the convention makes builds and IDEs
  easier to understand.
- `kotlin` distinguishes Kotlin sources from resources and other languages.
- `com/shraggen/diarium` mirrors the package declaration
  `package com.shraggen.diarium`. Kotlin does not technically require the
  directory to match, but JVM/Android tools and developers strongly expect it.
- `speech` groups the code by feature within that package.

Kotlin Multiplatform contributes meaningful platform and source-set structure,
while JVM reverse-domain packages add repetitive namespace folders. The
combination is deeper than the layouts used by languages without both concepts.

## Why keep the reverse-domain package?

Java introduced reverse-domain namespaces to avoid collisions in a global
class ecosystem. Kotlin inherited the JVM package model for interoperability.
It is dated-looking, but `com.shraggen.diarium` is already the Android
namespace and the stable public package for this project. Renaming it would
touch source files, tests, manifests, generated Room schemas, and potentially
saved or reflected type names while delivering little runtime or maintenance
value.

The Android application ID and Kotlin package can technically differ, and
Kotlin permits a short package such as `diarium`. That can be effective in a
greenfield private project. In Diarium, however, the migration cost and
compatibility risk outweigh the small navigation benefit.

## Boundaries make the depth useful

The directory layers expose decisions that the build enforces. A module shows
dependency direction. A source set shows which platforms can compile a file. A
package gives types a stable namespace. A feature name groups related domain or
technical concepts.

That distinction prevents Android frameworks and persistence implementations
from leaking into portable domain code. It also lets tests target the same
platform scope as the behaviour they protect. The repeated base package carries
less architectural meaning; IDE package compaction can hide it without changing
the source layout.

For exact module responsibilities, source sets, and placement rules, see the
[module and source-set reference](module-reference.md).
