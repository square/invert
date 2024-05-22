plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
}
dependencies {
    api(libs.ktor.client)

    implementation(project(":models"))
    implementation(libs.javax.inject)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
}
