import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.compose") version "1.12.2"
}

kotlin {
    compilerOptions {
        // ViTune's provider code uses Kotlin context parameters
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    // Compile ViTune's platform-independent modules straight from the parent repo,
    // so upstream fixes to the YouTube Music code arrive with a simple `git pull`.
    sourceSets.named("main") {
        kotlin.srcDirs(
            "src/main/kotlin",
            "../providers/common/src/main/kotlin",
            "../providers/innertube/src/main/kotlin",
            "../ktor-client-brotli/src/main/kotlin"
        )
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    implementation(libs.kotlin.coroutines)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:${libs.kotlin.coroutines.get().version}")
    implementation(libs.kotlin.datetime)

    // Same networking stack the Android app uses for its providers
    implementation(libs.ktor.http)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.client.serialization)
    implementation(libs.ktor.serialization.json)
    implementation(libs.brotli)
    implementation(libs.log4j)
    implementation("org.slf4j:slf4j-nop:2.0.17")

    // Album art
    implementation(libs.coil.compose)
    implementation(libs.coil.ktor)

    // Audio playback through the user's VLC installation
    implementation("uk.co.caprica:vlcj:4.8.3")
}

compose.desktop {
    application {
        mainClass = "app.vitune.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "ViTune Desktop"
            packageVersion = "1.0.0"
            description = "Desktop music player based on ViTune"
            modules("java.net.http", "java.naming", "jdk.unsupported")

            windows {
                menuGroup = "ViTune"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "6f1d2c3e-8b7a-4e3f-9a51-2c0d7e9b4a18"
            }
        }
    }
}
