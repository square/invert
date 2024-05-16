pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
//        maven { url = uri("https://oss.sonatype.org/service/local/repositories/snapshots/content") } // SNAPSHOT Versions for statik
    }
}

include(":collectors-anvil-dagger")
include(":collectors-kotlin-java-loc")
include(":custom-detekt-rules")
include(":custom-report")
include(":invert-models")
include(":invert-gradle-plugin")
include(":invert-report")
include(":owners-github-codeowners")
