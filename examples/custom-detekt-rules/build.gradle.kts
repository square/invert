plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnly("io.gitlab.arturbosch.detekt:detekt-api:1.23.6")
    compileOnly("io.gitlab.arturbosch.detekt:detekt-metrics:1.23.6")

    implementation("com.rickbusarow.statik:statik-api:0.1.0-SNAPSHOT")

    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:1.23.6")
    testImplementation(libs.kotlin.test)
}