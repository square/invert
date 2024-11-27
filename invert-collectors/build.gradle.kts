plugins {
  kotlin("jvm")
  alias(libs.plugins.vanniktech.maven.publish)
  alias(libs.plugins.dokka)
}

java {
  withSourcesJar()
}

dependencies {
  compileOnly(gradleApi())

  implementation(project(":invert-models"))
  implementation(project(":invert-gradle-plugin"))

  testImplementation(libs.kotlin.test)
}
