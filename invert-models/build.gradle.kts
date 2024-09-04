plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization")
  alias(libs.plugins.dokka)
  alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
  js {
    browser()
  }
  jvm {
    withSourcesJar()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(libs.kotlinx.serialization.core)
        api(libs.kotlinx.serialization.json)
      }
    }
  }
}
