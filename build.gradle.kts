// 루트 빌드 스크립트. Android 플러그인(AGP)은 Google Maven이 필요하므로
// -Pplaten.android=true 일 때만 classpath에 올린다(이 클라우드 컨테이너는 dl.google.com 차단).
buildscript {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    dependencies {
        if ((project.findProperty("platen.android") as String?) == "true") {
            classpath("com.android.tools.build:gradle:${libs.versions.agp.get()}")
        }
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
}

allprojects {
    group = "io.github.bigrirason_debug.platen"
    version = "0.1.0"
}
