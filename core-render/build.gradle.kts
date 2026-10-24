import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val platenAndroid = (project.findProperty("platen.android") as String?) == "true"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

if (platenAndroid) {
    apply(plugin = "com.android.library")
    // AGP 타입은 로컬(Android 미구성) 스크립트 컴파일 시 존재하지 않으므로 동적 구성한다.
    extensions.getByName("android").withGroovyBuilder {
        setProperty("namespace", "io.github.bigrirason_debug.platen.render")
        setProperty("compileSdk", 36)
        "defaultConfig" { setProperty("minSdk", 29) }
        "compileOptions" {
            setProperty("sourceCompatibility", JavaVersion.VERSION_17)
            setProperty("targetCompatibility", JavaVersion.VERSION_17)
        }
    }
}

kotlin {
    jvmToolchain(21)
    jvm("desktop")
    if (platenAndroid) {
        androidTarget {
            compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        }
    }
    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                api(project(":core-model"))
                api(project(":core-ink"))
                api(project(":brush"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        // JVM(데스크톱)과 Android가 공유하는 소스셋: java.* 사용 가능
        val jvmCommon by creating { dependsOn(commonMain) }
        val desktopMain by getting {
            dependsOn(jvmCommon)
            dependencies {

            }
        }
        if (platenAndroid) {
            val androidMain by getting {
                dependsOn(jvmCommon)
                dependencies {

                }
            }
        }
    }
}
