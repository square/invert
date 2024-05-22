plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.anvil)
}

dependencies {
    implementation(project(":scopes"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.anvil.annotations)
}
