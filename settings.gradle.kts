pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

include(":collectors:collectors-anvil-dagger")
include(":collectors:collectors-kotlin-java-loc")
include(":custom-detekt-rules")
include(":invert-models")
include(":invert-gradle-plugin")
include(":invert-report")
include(":owners:owners-github-codeowners")
