plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.anvil)
    alias(libs.plugins.detekt)
    application
}

dependencies {
//    implementation(project(":networking:fake"))
    implementation(project(":networking:impl"))
    implementation(project(":scopes"))

    implementation(libs.dagger)
    implementation(libs.javax.inject)
    implementation(libs.anvil.annotations)
    kapt(libs.dagger.compiler)
}

application {
    mainClass = "com.squareup.invert.examples.MainApp"
}
