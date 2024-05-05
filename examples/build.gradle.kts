import com.squareup.invert.InvertExtension
import com.squareup.invert.RealAnvilContributesBindingStatCollector

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    dependencies {
        val invertVersion = "0.0.1-dev-SNAPSHOT"
//        classpath("com.squareup.invert:invert-plugin:$invertVersion")
        classpath("com.squareup.invert:collectors-anvil-dagger:$invertVersion")
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
    id("com.squareup.invert") version "0.0.1-dev-SNAPSHOT"
}

invert {
    addStatCollector(RealAnvilContributesBindingStatCollector())
}
