plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization")
  alias(libs.plugins.jetbrains.compose)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.dokka)
  alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
  js {
    binaries.executable()
    val pluginResourcesDir =
      rootProject.layout.projectDirectory.file("invert-gradle-plugin/src/main/resources/META-INF").asFile
    browser {
      commonWebpackConfig {
        outputPath = pluginResourcesDir
      }
    }
  }
  sourceSets {
    val jsMain by getting {
      dependencies {
        api(project(":invert-models"))
        api(libs.kotlinx.serialization.json)
        api(libs.kotlinx.coroutines.core)
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
        api("org.jetbrains.kotlinx:kotlinx-html-js:0.9.1")
        api(compose.html.core)
        api(compose.runtime)
      }
    }
    jsMain.resources.srcDir(File(rootProject.rootDir, "invert-gradle-plugin/src/main/resources/META-INF"))
  }
}

compose {
  web {
  }
}

// Ensure that we generate the latest report JS when the plugin is built, so it can be included in the binary
project(":invert-report") {
  val isCI = System.getenv().containsKey("GITHUB_ACTIONS")
  val webpackTaskName = if (isCI) {
    "jsBrowserProductionWebpack"
  } else {
    "jsBrowserDevelopmentWebpack"
  }
  val reportWebpackTask = tasks.named(webpackTaskName)
  
  // Fix implicit dependency issues between webpack and compile sync tasks
  tasks.named("jsBrowserProductionWebpack").configure {
    dependsOn("jsDevelopmentExecutableCompileSync")
    dependsOn("jsProductionExecutableCompileSync")
  }
  
  tasks.named("jsBrowserDevelopmentWebpack").configure {
    dependsOn("jsDevelopmentExecutableCompileSync")
    dependsOn("jsProductionExecutableCompileSync")
  }
  
  project(":invert-gradle-plugin").tasks.named("processResources").configure {
    dependsOn(reportWebpackTask)
  }
}