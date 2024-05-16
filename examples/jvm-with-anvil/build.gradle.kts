import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.squareup.anvil") version "2.4.9"
    id("io.gitlab.arturbosch.detekt") version ("1.23.3")
}

tasks {
    withType<Detekt> {
        reports {
            custom {
                reportId = "sam"
                // This tells detekt, where it should write the report to,
                // you have to specify this file in the gitlab pipeline config.
                outputLocation.set(file(layout.buildDirectory.file("reports/detekt/invert.txt")))
            }
        }
    }
}

dependencies {
    implementation(project(":scopes"))
    implementation(libs.kotlinx.serialization.json)
    implementation("com.squareup.anvil:annotations:2.4.9")
    implementation("javax.inject:javax.inject:1")

    val invertVersion = "0.0.1-dev-SNAPSHOT"
    detektPlugins("com.squareup.invert:custom-detekt-rules:$invertVersion")
}
