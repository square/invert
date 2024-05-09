import com.squareup.invert.InvertExtension
import com.squareup.invert.LinesOfCodeStatCollector

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
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
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
