import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(21)
    jvm("desktop")
    sourceSets {
        val desktopMain by getting {
            kotlin.srcDir("src/main/kotlin")
            resources.srcDir("src/main/resources")
            dependencies {
                implementation(project(":ui"))
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.bigrirason_debug.platen.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Platen"
            packageVersion = project.version.toString()
            vendor = "Platen"
            description = "Platen — typewriter-flavoured handwriting notes"
            windows {
                menuGroup = "Platen"
                upgradeUuid = "6f0b2a3e-5c1d-4a8e-9f4b-2d7c1e9a0b11"
                perUserInstall = true
            }
        }
    }
}
