pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

include(":jvm-with-anvil")
include(":scopes")
includeBuild("../") // Include all of the invert plugin repo