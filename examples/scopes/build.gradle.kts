plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.squareup.anvil") version "2.4.9"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation("com.squareup.anvil:annotations:2.4.9")
    implementation("javax.inject:javax.inject:1")
}
