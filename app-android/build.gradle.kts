// AGP 는 루트 buildscript classpath(-Pplaten.android=true 일 때)로 들어오므로 plugins 블록이 아니라 명령형 apply 를 쓴다.
// (plugins 블록으로 다시 요청하면 "already on the classpath with an unknown version" 오류)
plugins {
    // KGP 는 루트에서 multiplatform 플러그인으로 이미 classpath 에 있으므로 버전 없이 id 로만 적용한다.
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

apply(plugin = "com.android.application")

extensions.configure<com.android.build.api.dsl.ApplicationExtension>("android") {
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
        getByName("release") {
            isMinifyEnabled = false
            // 사이드로드 전용: 릴리스 서명 키는 CI Secrets 로 주입 예정(없으면 디버그 서명)
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":ui"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.compose.material3)
}
