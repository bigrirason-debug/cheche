// Maven Central 직접 접근이 이 환경에서 간헐 429/404를 내므로 Google 운영 미러를 1순위로 둔다(내용 동일, 읽기 전용).

pluginManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}
dependencyResolutionManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        google()
    }
}
rootProject.name = "platen"

include(":core-model", ":core-ink", ":core-render", ":brush", ":sync", ":pdf", ":ui", ":app-desktop")
// Android 타깃은 Google Maven이 필요하다. 이 클라우드 컨테이너에서는 차단되어 있어 CI(-Pplaten.android=true)에서만 구성한다.
if ((extra.properties["platen.android"] as String?) == "true") {
    include(":app-android")
}
