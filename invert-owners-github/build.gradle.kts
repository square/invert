plugins {
  kotlin("jvm")
  id("java-gradle-plugin")
  alias(libs.plugins.vanniktech.maven.publish)
  alias(libs.plugins.dokka)
}

java {
  withSourcesJar()
}

dependencies {
  implementation(project(":invert-gradle-plugin"))

  testImplementation(libs.truth)
  testImplementation(libs.kotlin.test)
}
