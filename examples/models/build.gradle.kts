plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.anvil)
    alias(libs.plugins.detekt)
}

dependencies {
    implementation(project(":scopes"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.anvil.annotations)
}
