pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

include(":jvm-with-anvil")
includeBuild("../") // Include all of the invert plugin repo