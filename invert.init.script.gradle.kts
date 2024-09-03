import com.squareup.invert.InvertGradlePlugin
import com.squareup.invert.InvertExtension

initscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        // SNAPSHOT Versions
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
        }
    }
    dependencies {
        val invertVersion = "+"
        classpath("com.squareup.invert:invert-gradle-plugin:$invertVersion")
    }
}

apply<InvertInitScriptPlugin>()

class InvertInitScriptPlugin : Plugin<Gradle> {

    override fun apply(gradle: Gradle) {
        gradle.settingsEvaluated {
            gradle.rootProject {
                buildscript {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                        mavenLocal()
                        // SNAPSHOT Versions
                        maven {
                            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
                        }
                    }

                    dependencies {
                        val invertVersion = "0.0.2-dev"
                        classpath("com.squareup.invert:invert-gradle-plugin:$invertVersion")
                    }
                }
            }

            gradle.rootProject {
                afterEvaluate {
                    plugins.apply(com.squareup.invert.InvertGradlePlugin::class.java)
                    this.extensions.getByType(com.squareup.invert.InvertExtension::class.java).apply {
//                        ownershipCollector(...)
//                        addStatCollector(...)
                    }
                }
            }
        }
    }
}