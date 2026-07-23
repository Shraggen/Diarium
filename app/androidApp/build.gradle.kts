import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

val appVersionName = rootProject.file("version.txt").readText().trim()
val appVersionParts = appVersionName.split('.').map { part ->
    part.toIntOrNull()
        ?: error("version.txt must contain a numeric MAJOR.MINOR.PATCH version.")
}

require(appVersionParts.size == 3) {
    "version.txt must contain exactly MAJOR.MINOR.PATCH."
}
require(appVersionParts.all { it in 0..999 }) {
    "Each version component must be between 0 and 999."
}

val appVersionCode =
    appVersionParts[0] * 1_000_000 +
        appVersionParts[1] * 1_000 +
        appVersionParts[2]

val releaseStoreFile = providers.gradleProperty("DIARIUM_RELEASE_STORE_FILE")
val releaseStorePassword = providers.gradleProperty("DIARIUM_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = providers.gradleProperty("DIARIUM_RELEASE_KEY_ALIAS")
val releaseKeyPassword = providers.gradleProperty("DIARIUM_RELEASE_KEY_PASSWORD")
val releaseSigningProperties = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
)
val configuredReleaseSigningProperties = releaseSigningProperties.count { it.isPresent }

require(
    configuredReleaseSigningProperties == 0 ||
        configuredReleaseSigningProperties == releaseSigningProperties.size,
) {
    "Release signing requires store file, store password, key alias, and key password."
}

val releaseSigningConfigured =
    configuredReleaseSigningProperties == releaseSigningProperties.size

detekt {
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.app.sharedUI)

    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.android.vad.silero)
    ksp(libs.androidx.room.compiler)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.junit4)
    androidTestUtil(libs.androidx.test.orchestrator)
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.shraggen.diarium"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.shraggen.diarium"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include(
                "arm64-v8a",
                "armeabi-v7a",
                "x86_64",
                "x86",
            )
            isUniversalApk = false
        }
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}
