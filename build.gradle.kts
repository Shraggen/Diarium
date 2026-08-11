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
