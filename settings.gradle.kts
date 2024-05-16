pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
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
