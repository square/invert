rootProject.name = "Invert Examples"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

include(":app")
include(":networking:api")
include(":networking:fake")
include(":networking:impl")
include(":models")
include(":scopes")
includeBuild("../") // Include all of the invert plugin repo
