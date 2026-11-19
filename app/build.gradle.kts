import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Release signing: put storeFile / storePassword / keyAlias / keyPassword in keystore.properties
// (git-ignored; CI writes it from secrets). Without it, release builds fall back to the public dev key,
// which is fine for local testing but must never be used for published builds.
val releaseProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasReleaseKey = releaseProps.getProperty("storeFile") != null

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.padbridge"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.padbridge"
        minSdk = 28
        targetSdk = 36
        versionCode = 28
        versionName = "1.3.3"
    }

    // github: everything, including experimental root mode. play: Play Store build without root features.
    flavorDimensions += "dist"
    productFlavors {
        create("github") {
            dimension = "dist"
            buildConfigField("boolean", "ROOT_FEATURES", "true")
            buildConfigField("String", "DIST", "\"github\"")
            // UPI donations to the developer. Play counts tips as digital purchases (Play Billing only), so not there.
            buildConfigField("boolean", "DONATE", "true")
        }
        create("play") {
            dimension = "dist"
            buildConfigField("boolean", "ROOT_FEATURES", "false")
            buildConfigField("String", "DIST", "\"play\"")
            buildConfigField("boolean", "DONATE", "false")
        }
    }

    // One shared, non-secret dev key so builds from any machine (workbench, Mac, Spark) install over
    // each other. Android refuses updates signed with a different key. Not for Play Store releases.
    signingConfigs {
        create("dev") {
            storeFile = rootProject.file("keystore/padbridge-dev.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (hasReleaseKey) create("release") {
            storeFile = rootProject.file(releaseProps.getProperty("storeFile"))
            storePassword = releaseProps.getProperty("storePassword")
            keyAlias = releaseProps.getProperty("keyAlias")
            keyPassword = releaseProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("dev")
            // Installs next to the release app (own package, own label), so testing never replaces it.
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            manifestPlaceholders["appLabel"] = "PadBridge Dev"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            manifestPlaceholders["appLabel"] = "PadBridge"
            signingConfig = signingConfigs.getByName(if (hasReleaseKey) "release" else "dev")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    // Material 3 Expressive APIs (MaterialExpressiveTheme, ShortNavigationBar, ToggleButton, LoadingIndicator…)
    implementation("androidx.compose.material3:material3:1.5.0-rc01")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.13.0")
    // Play build only: Google Play's in-app updates (talks to the Play Store app, no INTERNET permission).
    "playImplementation"("com.google.android.play:app-update:2.1.0")
}

/** Bundles the repository's CHANGELOG.md as an asset, for "What's new" in Settings › About. */
abstract class ChangelogAsset : DefaultTask() {
    @get:InputFile abstract val source: RegularFileProperty
    @get:OutputDirectory abstract val out: DirectoryProperty

    @TaskAction fun copy() {
        source.get().asFile.copyTo(out.get().file("CHANGELOG.md").asFile, overwrite = true)
    }
}

androidComponents {
    onVariants { v ->
        val task = tasks.register<ChangelogAsset>("changelogAsset${v.name.replaceFirstChar { it.uppercase() }}") {
            source.set(rootProject.layout.projectDirectory.file("CHANGELOG.md"))
        }
        v.sources.assets?.addGeneratedSourceDirectory(task, ChangelogAsset::out)
    }
}
