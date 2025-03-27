import com.squareup.invert.InvertExtension
import com.squareup.invert.InvertGradlePlugin

initscript {
  repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
    // SNAPSHOT Versions
    maven {
      url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
  }
  dependencies {
    val invertVersion = "0.0.4-dev-SNAPSHOT"
    classpath("com.squareup.invert:invert-gradle-plugin:$invertVersion")
    classpath("com.squareup.invert:invert-collectors:$invertVersion")
    classpath("com.squareup.invert:invert-owners-github:$invertVersion")
  }
}

apply<InvertInitScriptPlugin>()

class InvertInitScriptPlugin : Plugin<Gradle> {

  override fun apply(gradle: Gradle) {
    gradle.settingsEvaluated {
      gradle.rootProject {
        buildscript {
          repositories {
            mavenLocal()
            mavenCentral()
            gradlePluginPortal()
            // SNAPSHOT Versions
            maven {
              url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
            }
          }

          dependencies {
            val invertVersion = "0.0.4-dev-SNAPSHOT"
            classpath("com.squareup.invert:invert-gradle-plugin:$invertVersion")
            classpath("com.squareup.invert:invert-collectors:$invertVersion")
            classpath("com.squareup.invert:invert-owners-github:$invertVersion")
          }
        }
      }

      gradle.rootProject {
        afterEvaluate {
          plugins.apply(com.squareup.invert.InvertGradlePlugin::class.java)
          this.extensions.getByType(com.squareup.invert.InvertExtension::class.java).apply {
            ownershipCollector(com.squareup.invert.owners.GitHubCodeOwnersInvertOwnershipCollector)
            addStatCollector(
              com.squareup.invert.collectors.linesofcode.LinesOfCodeStatCollector(
                name = "Kotlin",
                fileExtensions = listOf("kt", "kts"),
              )
            )
            addStatCollector(
              com.squareup.invert.collectors.linesofcode.LinesOfCodeStatCollector(
                name = "Java",
                fileExtensions = listOf("java"),
              )
            )
          }
        }
      }
    }
  }
}