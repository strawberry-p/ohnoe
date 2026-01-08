import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("app.cash.sqldelight:coroutines-extensions:2.1.0")
            implementation("io.ktor:ktor-client-core:3.3.3")
            implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
            implementation("io.github.vinceglb:confettikit:0.7.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.macos_arm64)
            implementation(compose.desktop.windows_x64)
            implementation(compose.desktop.linux_x64)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("app.cash.sqldelight:sqlite-driver:2.1.0")
            implementation("com.google.genai:google-genai:1.32.0")
            implementation("io.github.kdroidfilter:knotify:0.4.3")
            implementation("io.ktor:ktor-client-apache5:3.3.3")
            implementation("com.github.sarxos:webcam-capture:0.3.12")
        }
    }
}

compose.desktop {
    application {
        mainClass = "tech.tarakoshka.ohnoe_desktop.MainKt"

        buildTypes.release.proguard {
            isEnabled = false
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "tech.tarakoshka.ohnoe_desktop"
            packageVersion = "1.0.0"

            modules(
                "java.sql", "java.desktop", "java.logging", "java.net.http", "jdk.crypto.ec"
            )
        }
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("tech.tarakoshka.ohnoe_desktop")
        }
    }
}