plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    kotlin("plugin.serialization")
    alias(libs.plugins.vanniktech.maven.publish)
}

dependencies {
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.compiler.embeddable)
    implementation(project(":invert-models"))
    implementation(project(":invert-gradle-plugin"))
    testImplementation(libs.kotlin.test)

    testImplementation("com.google.truth:truth:1.0.1")
    val junit5 = "5.3.1"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5")
    val spek = "2.0.7"
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spek")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spek")
    testImplementation(kotlin("reflect"))
}
