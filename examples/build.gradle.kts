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
    val invertVersion = "0.0.4-dev-SNAPSHOT"
    classpath("com.squareup.invert:invert-gradle-plugin:$invertVersion")
    classpath("com.squareup.invert:invert-collectors:$invertVersion")
    classpath("com.squareup.invert:invert-owners-github:$invertVersion")
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
  ownershipCollector(com.squareup.invert.owners.GitHubCodeOwnersInvertOwnershipCollector)
  addStatCollector(
    com.squareup.invert.collectors.linesofcode.LinesOfCodeStatCollector(
      name = "Kotlin",
      fileExtensions = listOf("kt", "kts"),
    )
  )
  addStatCollector(
    com.squareup.invert.collectors.contains.InvertContainsStatCollector(
      statKey = "wildcard-imports",
      statTitle = "Wildcard Imports",
      linePredicate = { it.contains("import") && it.contains("*") },
      filePredicate = { it.extension == "kt" || it.extension == "kts" },
    )
  )
}

/**
 * Will copy the report js files into invert so we can use them in dev cycles
 */
tasks.register("copyJsFiles") {
  doLast {
    copy {
      from(fileTree("build/reports/invert/js") { include("*.js") }) // Replace "src/js" with your source directory
      into("../invert-report/src/jsMain/resources/js") // Replace "build/js" with your target directory
    }
  }
}

afterEvaluate {
  rootProject.tasks.findByName("invert")!!.finalizedBy("copyJsFiles")
}
