plugins {
  kotlin("multiplatform")
  id("kotlinx-serialization")
  id("com.vanniktech.maven.publish.base")
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
