// AGP·KGP 는 루트 buildscript classpath 에 있으므로(-Pplaten.android=true) 명령형으로 적용한다.
// plugins{} 로 다시 요청하면 "already on the classpath with an unknown version" 오류가 나고,
// 명령형 적용이라 타입 안전 접근자(implementation, android{}, kotlin{})가 생성되지 않아 문자열/extensions 로 구성한다.
plugins {
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

apply(plugin = "com.android.application")
apply(plugin = "org.jetbrains.kotlin.android")

extensions.configure<com.android.build.api.dsl.ApplicationExtension>("android") {
    namespace = "io.github.bigrirason_debug.platen"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.bigrirason_debug.platen"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = project.version.toString()
        // Credential Manager 용 Google 웹 클라이언트 ID(공개 식별자). CI 는 PLATEN_WEB_CLIENT_ID 시크릿을 -Pplaten.webClientId 로 넘긴다.
        resValue("string", "platen_web_client_id", (project.findProperty("platen.webClientId") as String?) ?: "")
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
    packaging {
        resources.excludes += setOf("META-INF/INDEX.LIST", "META-INF/io.netty.versions.properties", "META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*")
    }
}

extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension>("kotlin") {
    jvmToolchain(21)
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    "implementation"(project(":ui"))
    "implementation"(libs.androidx.activity.compose)
    "implementation"(libs.kotlinx.coroutines.android)
    "implementation"(libs.compose.material3)
    "implementation"(libs.ktor.client.okhttp)
}
