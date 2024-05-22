plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.anvil)
    alias(libs.plugins.detekt)
}

anvil {
    generateDaggerFactories = true
}

dependencies {
    api(project(":networking:api"))
    implementation(project(":scopes"))
    implementation(project(":models"))

    implementation(libs.anvil.annotations)
    implementation(libs.javax.inject)
    implementation(libs.dagger)
}
