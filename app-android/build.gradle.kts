plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.bigrirason_debug.platen"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.bigrirason_debug.platen"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = project.version.toString()
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            // 사이드로드 전용: 서명 키는 CI Secrets로 주입(없으면 디버그 서명)
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(21) }
}

dependencies {
    implementation(project(":ui"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(compose.material3)
}
