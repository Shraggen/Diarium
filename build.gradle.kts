plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.cyclonedx) apply false
}

allprojects {
    dependencyLocking {
        lockAllConfigurations()
    }
}

val installGitHooks = tasks.register<Exec>("installGitHooks") {
    group = "development"
    description = "Configures git hooks path to .githooks"

    val isWindows = org.gradle.internal.os.OperatingSystem.current().isWindows
    if (isWindows) {
        commandLine("cmd", "/c", "git config core.hooksPath .githooks")
    } else {
        commandLine("git", "config", "core.hooksPath", ".githooks")
    }
}

gradle.projectsEvaluated {
    subprojects {
        tasks.matching { it.name == "build" || it.name == "preBuild" }.configureEach {
            dependsOn(installGitHooks)
        }
    }
}

subprojects {
    pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
        dependencies {
            "detektPlugins"(libs.detekt.formatting)
            "detektPlugins"(libs.detekt.rules.compose)
        }
    }
}

// Pitest has no Kotlin Multiplatform integration, so mutation testing reuses the
// classpath of each module's existing JVM test task and mutates only that module's
// production output on it. Report-only: no threshold.
val mutationTestedModules = mapOf(
    ":core" to ("jvmTest" to "classes/kotlin/jvm/main"),
    ":app:sharedLogic" to (
        "testAndroidHostTest" to
            "intermediates/runtime_library_classes_jar/androidMain/*/classes.jar"
        ),
)

subprojects {
    val (testTaskName, productionOutput) = mutationTestedModules[path] ?: return@subprojects
    val pitest = configurations.create("pitest")
    dependencies {
        pitest(rootProject.libs.pitest.commandLine)
    }

    tasks.register<JavaExec>("mutationTest") {
        group = "verification"
        description = "Runs Pitest mutation testing against the $testTaskName suite."

        val testTask = tasks.named<Test>(testTaskName)
        val reportDir = layout.buildDirectory.dir("reports/pitest")
        dependsOn(testTask)
        javaLauncher.set(testTask.flatMap { it.javaLauncher })
        classpath(pitest, testTask.map { it.classpath })
        mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")
        outputs.dir(reportDir)

        val sourceDirs = file("src")
            .listFiles { dir -> dir.name.endsWith("Main") }
            .orEmpty()
            .joinToString(",") { it.resolve("kotlin").path }
        args(
            // Tests share production packages, so select mutable code by location.
            "--mutableCodePaths=${layout.buildDirectory.get().asFile}/$productionOutput",
            "--targetClasses=com.shraggen.diarium.*",
            "--targetTests=*Test",
            "--excludedTestClasses=*ArchitectureFitnessTest",
            // Kotlin data classes generate these; mutating them only adds noise.
            "--excludedMethods=equals,hashCode,toString,component*,copy,copy\$default",
            // Kotlin null-check intrinsics and coroutine result unwrapping are
            // compiler-generated, not domain behavior.
            "--avoidCallsTo=kotlin.jvm.internal,kotlin.ResultKt",
            "--sourceDirs=$sourceDirs",
            "--reportDir=${reportDir.get().asFile}",
            "--outputFormats=HTML,XML",
            "--timestampedReports=false",
            "--threads=${Runtime.getRuntime().availableProcessors()}",
        )
    }
}
