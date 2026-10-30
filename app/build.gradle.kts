plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "io.github.marozauartsiom.tvlauncher"
    compileSdk { version = release(37) }

    defaultConfig {
        applicationId = "io.github.marozauartsiom.tvlauncher"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    androidResources {
        generateLocaleConfig = true
    }

    signingConfigs {
        // Every debug build (also the CI ones) is signed with the same project key, so a newer APK installs over an older one.
        // It is a throwaway key with the public password "android"; the release key is never in the repository.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                // Robolectric reflects into the JDK; JDK 17+ needs these modules opened.
                test.jvmArgs(
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
                    "--add-opens=java.base/java.nio=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                )
                test.maxHeapSize = "2g"
            }
        }
    }
}

roborazzi {
    // Goldens live next to the tests; each test task gets its own folder (avoids a Gradle 9 race).
    outputDir.set(layout.projectDirectory.dir("src/test/snapshots"))
    separateOutputDirs.set(true)
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3) // only for text fields; tv-material has none
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.tv.material)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.core.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.palette.ktx)
    implementation(libs.profileinstaller)
    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)
    implementation(libs.dadb)
    implementation(libs.zxing.core)
    implementation(libs.jsch)

    // The profile recorded on the TV by :baselineprofile is merged into the release build (src/release/generated/baselineProfiles).
    baselineProfile(project(":baselineprofile"))

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}

/**
 * The phone page (`assets/remote/`) styles itself with the launcher's own theme tokens. They are copied from
 * the approved mockups at build time, so there is one source of truth (`ThemeCssSyncTest` keeps the Kotlin
 * port in step with the same file).
 */
abstract class CopyRemoteThemeAssets : DefaultTask() {
    @get:InputFile abstract val themesCss: RegularFileProperty
    @get:InputDirectory abstract val backgrounds: DirectoryProperty
    @get:InputFile abstract val placeholder: RegularFileProperty
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val out = outputDir.get().asFile.resolve("remote")
        out.deleteRecursively()
        out.resolve("assets/bg").mkdirs()
        themesCss.get().asFile.copyTo(out.resolve("themes.css"), overwrite = true)
        backgrounds.get().asFile.listFiles { f -> f.extension == "svg" }!!.forEach { it.copyTo(out.resolve("assets/bg/${it.name}"), overwrite = true) }
        placeholder.get().asFile.copyTo(out.resolve("assets/landscape-placeholder.svg"), overwrite = true)
    }
}

val remoteThemeAssets = tasks.register<CopyRemoteThemeAssets>("remoteThemeAssets") {
    themesCss.set(rootProject.layout.projectDirectory.file("design/mockups/themes.css"))
    backgrounds.set(rootProject.layout.projectDirectory.dir("design/mockups/assets/bg"))
    placeholder.set(rootProject.layout.projectDirectory.file("design/mockups/assets/landscape-placeholder.svg"))
    outputDir.set(layout.buildDirectory.dir("generated/remoteAssets"))
}

androidComponents {
    onVariants { variant -> variant.sources.assets?.addGeneratedSourceDirectory(remoteThemeAssets, CopyRemoteThemeAssets::outputDir) }
}
