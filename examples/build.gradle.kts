buildscript {
  repositories {
    mavenLocal()
    mavenCentral()
    google()
    gradlePluginPortal()
    maven { url = uri("https://oss.sonatype.org/service/local/repositories/snapshots/content") }
//        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") } // SNAPSHOT Versions
  }
  dependencies {
    val invertVersion = "0.0.2-dev"
    classpath("com.squareup.invert:invert-gradle-plugin:$invertVersion")
//        classpath("com.squareup.invert:collectors-anvil-dagger:$invertVersion")
//        classpath("com.squareup.invert:collectors-kotlin-java-loc:$invertVersion")
//        classpath("com.squareup.invert:owners-github-codeowners:$invertVersion")
  }
}

allprojects {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
    maven { url = uri("https://oss.sonatype.org/service/local/repositories/snapshots/content") }
  }
}

plugins {
    id("com.squareup.invert")
}

invert {
//    ownershipCollector(com.squareup.invert.GitHubCodeOwnersInvertOwnershipCollector)
//    addStatCollector(com.squareup.invert.DiProvidesAndInjectsStatCollector())
//    addStatCollector(com.squareup.invert.suppress.SuppressionsStatCollector())
//    addStatCollector(com.squareup.invert.LinesOfCodeStatCollector())
}
