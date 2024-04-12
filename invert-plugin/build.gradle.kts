plugins {
  kotlin("jvm")
  id("kotlinx-serialization")
  id("java-gradle-plugin")
  id("com.vanniktech.maven.publish.base")
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

dependencies {
  api(project(":invert-models"))

  implementation(libs.kotlinx.serialization.json)
}

kotlin {
  sourceSets {
    val main by getting {
      resources.srcDir("src/main/resources")
    }
  }
}
