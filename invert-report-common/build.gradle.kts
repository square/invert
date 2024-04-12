plugins {
  kotlin("multiplatform")
  id("org.jetbrains.compose")
  kotlin("plugin.serialization")
  id("com.vanniktech.maven.publish.base")
}

kotlin {
  js {
    browser()
  }

  sourceSets {
    val jsMain by getting {
      dependencies {
        api(project(":invert-models"))
        api(libs.kotlinx.serialization.json)
        api(libs.kotlinx.coroutines.core)

        implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.9.1")
        implementation(compose.html.core)
        implementation(compose.runtime)
      }
    }
  }
}

compose {
  web {
  }
}
