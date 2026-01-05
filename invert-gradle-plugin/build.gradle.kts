plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("java-gradle-plugin")
  alias(libs.plugins.dokka)
  alias(libs.plugins.vanniktech.maven.publish)
}

afterEvaluate {
  tasks.findByName("publishMavenPublicationToMavenCentralRepository")?.let { publishTask ->
    tasks.findByName("signPluginMavenPublication")?.let { signTask ->
      publishTask.mustRunAfter(signTask)
    }
  }
  tasks.findByName("publishPluginMavenPublicationToMavenCentralRepository")?.let { publishTask ->
    tasks.findByName("signMavenPublication")?.let { signTask ->
      publishTask.mustRunAfter(signTask)
    }
  }
}

gradlePlugin {
  plugins {
    this.create("invertPlugin").apply {
      id = "com.squareup.invert"
      implementationClass = "com.squareup.invert.InvertGradlePlugin"
    }
  }
}

java {
  withSourcesJar()
}

tasks.named<Jar>("sourcesJar") {
  // Allow overwriting of HTML & JS files in resources/META-INF during publishing
  duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

dependencies {
  api(gradleApi())
  api(project(":invert-models"))

  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okio)
  implementation(libs.detekt.sarif4k)

  testImplementation(libs.kotlin.test)
}

kotlin {
  sourceSets {
    val main by getting {
      resources.srcDir("src/main/resources")
    }
  }
}

// Allow overwriting of HTML & JS files in resources/META-INF during publishing
tasks {
  named<Copy>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }
}
