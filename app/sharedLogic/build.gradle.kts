import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.detekt)
}

detekt {
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())

    // Desktop JVM target: runs shared tests and laptop model evaluation.
    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
        }
    }
    
    android {
       namespace = "com.shraggen.diarium.sharedLogic"
       compileSdk {
           version = release(libs.versions.android.compileSdk.get().toInt()) {
               minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
           }
       }
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    
    sourceSets {
        commonMain.dependencies {
            api(projects.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.llamatik)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotest.property)
        }
    }
}

// Model evaluation (docs/testing.md): scores the golden corpus against a real
// GGUF model. Opt-in and report-only; never part of jvmTest or allTests.
val modelEvalClasses = "*ModelEval"
val jvmTest = tasks.named<Test>("jvmTest") {
    filter.excludeTestsMatching(modelEvalClasses)
}

tasks.register<Test>("modelEval") {
    group = "verification"
    description = "Scores the golden corpus against a GGUF model: -Pdiarium.llmModel=<path.gguf>"

    val repositoryRoot = rootDir
    val llmModel = providers.gradleProperty("diarium.llmModel").map { path ->
        val expanded = if (path.startsWith("~/")) System.getProperty("user.home") + path.drop(1) else path
        repositoryRoot.resolve(expanded).absolutePath
    }
    val reportDir = layout.buildDirectory.dir("reports/model-eval")

    testClassesDirs = jvmTest.get().testClassesDirs
    classpath = jvmTest.get().classpath
    javaLauncher = jvmTest.flatMap { it.javaLauncher }
    useJUnit()
    filter.includeTestsMatching(modelEvalClasses)
    // Llamatik holds one global, non-thread-safe model per process.
    maxParallelForks = 1
    maxHeapSize = "2g"
    testLogging.showStandardStreams = true
    outputs.upToDateWhen { false }
    outputs.dir(reportDir)

    systemProperty("diarium.reportDir", reportDir.get().asFile.absolutePath)
    listOf("diarium.promptMode", "diarium.sampling", "diarium.evalRepeats").forEach { name ->
        providers.gradleProperty(name).orNull?.let { systemProperty(name, it) }
    }
    llmModel.orNull?.let { systemProperty("diarium.llmModel", it) }

    doFirst {
        val model = llmModel.orNull?.let(::File) ?: throw GradleException(
            "modelEval needs a command model: ./gradlew :app:sharedLogic:modelEval -Pdiarium.llmModel=<path.gguf>",
        )
        if (!model.isFile) throw GradleException("modelEval command model not found: $model")
    }
}
