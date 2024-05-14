pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

include(":jvm-with-anvil")
include(":custom-detekt-rules")
include(":scopes")
includeBuild("../") // Include all of the invert plugin repo