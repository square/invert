import com.squareup.invert.InvertExtension

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    dependencies {
        val invertVersion = "0.0.1-dev-SNAPSHOT"
        classpath("com.squareup.invert:invert-gradle-plugin:$invertVersion")
        classpath("com.squareup.invert:collectors-anvil-dagger:$invertVersion")
        classpath("com.squareup.invert:collectors-kotlin-java-loc:$invertVersion")
        classpath("com.squareup.invert:owners-github-codeowners:$invertVersion")
        classpath("com.squareup.invert:owners-github-codeowners:$invertVersion")
        classpath("com.squareup.invert:custom-detekt-rules:0.0.1-dev-SNAPSHOT")
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven { url = uri("https://oss.sonatype.org/service/local/repositories/snapshots/content") } // SNAPSHOT Versions for statik
    }
}

plugins {
    id("com.squareup.invert")
}

invert {
    ownershipCollector(com.squareup.invert.GitHubCodeOwnersInvertOwnershipCollector)
    addStatCollector(com.squareup.invert.RealAnvilContributesBindingStatCollector())
    addStatCollector(com.squareup.invert.LinesOfCodeStatCollector())
}
