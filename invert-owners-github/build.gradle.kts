plugins {
  kotlin("jvm")
  id("java-gradle-plugin")
  alias(libs.plugins.vanniktech.maven.publish)
  alias(libs.plugins.dokka)
}

afterEvaluate {
  tasks.findByName("signPluginMavenPublication")?.let { signTask ->
    tasks.named("publishMavenPublicationToMavenCentralRepository") {
      mustRunAfter(signTask)
    }
  }
  tasks.findByName("signMavenPublication")?.let { signTask ->
    tasks.named("publishPluginMavenPublicationToMavenCentralRepository") {
      mustRunAfter(signTask)
    }
  }
}

java {
  withSourcesJar()
}

dependencies {
  implementation(project(":invert-gradle-plugin"))

  testImplementation(libs.truth)
  testImplementation(libs.kotlin.test)
}
