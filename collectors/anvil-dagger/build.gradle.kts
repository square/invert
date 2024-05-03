import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.vanniktech.maven.publish)
}


dependencies {
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.compiler.embeddable)
    
    implementation(project(":invert-models"))
    implementation(project(":invert-plugin"))
    
    testImplementation(libs.kotlin.test)
}

mavenPublishing {
    pomFromGradleProperties()
    signAllPublications()
    publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
}
