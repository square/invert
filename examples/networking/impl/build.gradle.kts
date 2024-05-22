plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.anvil)
}

anvil {
    generateDaggerFactories = true
}

dependencies {
    api(project(":models"))
    api(project(":networking:api"))
    api(project(":scopes"))
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.anvil.annotations)
    implementation(libs.dagger)
    implementation(libs.kotlin.reflect)
    implementation(libs.javax.inject)
}