plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    alias(libs.plugins.vanniktech.maven.publish)
}

dependencies {
    implementation(project(":invert-gradle-plugin"))

    testImplementation(libs.truth)
    testImplementation(libs.kotlin.test)
}
