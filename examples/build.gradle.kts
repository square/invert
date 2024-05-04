//import com.squareup.invert.InvertExtension

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
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

invert {}
