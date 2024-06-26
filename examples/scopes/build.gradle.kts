plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.anvil.annotations)
    implementation(libs.javax.inject)
}
