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
  // Explicit JVM configuration is needed for composite builds (includeBuild) to correctly
  // resolve the JVM variant of this Kotlin Multiplatform dependency on the plugin classpath.
  api(project(path = ":invert-models", configuration = "jvmRuntimeElements"))

  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okio)
  compileOnly(libs.detekt.sarif4k) {
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-io-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-io-core-jvm")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-bom")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core-jvm")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json-jvm")
  }
  runtimeOnly(libs.detekt.sarif4k)
  testCompileOnly(libs.detekt.sarif4k) {
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-io-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-io-core-jvm")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-bom")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core-jvm")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json-jvm")
  }

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
