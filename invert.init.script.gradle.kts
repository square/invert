import com.squareup.invert.InvertGradlePlugin
import com.squareup.invert.InvertExtension

initscript {
    repositories {
        mavenLocal()
        mavenCentral()
        // SNAPSHOT Versions
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
        }
        // SNAPSHOT Versions for statik
        maven {
            url = uri("https://oss.sonatype.org/service/local/repositories/snapshots/content")
        }
    }
    dependencies {
        val invertVersion = "+"
        classpath("com.squareup.invert:invert-gradle-plugin:$invertVersion")
        classpath("com.squareup.invert:collectors-anvil-dagger:$invertVersion")
        classpath("com.squareup.invert:collectors-kotlin-java-loc:$invertVersion")
        classpath("com.squareup.invert:owners-github-codeowners:$invertVersion")
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
                        google()
                        mavenLocal()
                        // SNAPSHOT Versions
                        maven {
                            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
                        }
                        // SNAPSHOT Versions for statik
                        maven {
                            url = uri("https://oss.sonatype.org/service/local/repositories/snapshots/content")
                        }
                    }

                    dependencies {
                        val invertVersion = "0.0.1-dev-SNAPSHOT"
                        classpath("com.squareup.invert:invert-gradle-plugin:$invertVersion")
                        classpath("com.squareup.invert:collectors-anvil-dagger:$invertVersion")
                        classpath("com.squareup.invert:collectors-kotlin-java-loc:$invertVersion")
                        classpath("com.squareup.invert:owners-github-codeowners:$invertVersion")
                    }
                }
            }

            gradle.rootProject {
                afterEvaluate {
                    plugins.apply(com.squareup.invert.InvertGradlePlugin::class.java)
                    this.extensions.getByType(com.squareup.invert.InvertExtension::class.java).apply {
                        ownershipCollector(com.squareup.invert.GitHubCodeOwnersInvertOwnershipCollector)
                        addStatCollector(com.squareup.invert.RealAnvilContributesBindingStatCollector())
                        addStatCollector(com.squareup.invert.suppress.SupressionsStatCollector())
                        addStatCollector(com.squareup.invert.LinesOfCodeStatCollector())
                    }
                }
            }
        }
    }
}