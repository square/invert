//
//class EnterpriseRepositoryPlugin : Plugin<Gradle> {
//
//    override fun apply(gradle: Gradle) {
//        // ONLY USE ENTERPRISE REPO FOR DEPENDENCIES
//        gradle.allprojects {
//            repositories {
//
////                // Remove all repositories not pointing to the enterprise repository url
////                all {
////                    if (this !is MavenArtifactRepository || url.toString() != ENTERPRISE_REPOSITORY_URL) {
////                        project.logger.lifecycle("Repository ${(this as? MavenArtifactRepository)?.url ?: name} removed. Only $ENTERPRISE_REPOSITORY_URL is allowed")
////                        remove(this)
////                    }
////                }
//
//                // add the enterprise repository
//                add(mavenLocal())
//            }
//
//            dependencies {
////            add("classpath", "")
//            }
//        }
//
//        gradle.plugins.apply("com.squareup.invert")
//
//
//    }
//}
//
//import com.squareup.invert.InvertExtension
//
//buildscript {
//    repositories {
//        mavenLocal()
//        mavenCentral()
//        google()
//        gradlePluginPortal()
//    }
//    dependencies {
//        val invertVersion = "0.0.1-dev-SNAPSHOT"
//        classpath("com.squareup.invert:invert-gradle-plugin:$invertVersion")
//        classpath("com.squareup.invert:collectors-anvil-dagger:$invertVersion")
//        classpath("com.squareup.invert:collectors-kotlin-java-loc:$invertVersion")
//        classpath("com.squareup.invert:owners-github-codeowners:$invertVersion")
//    }
//}
//
//allprojects {
//    repositories {
//        mavenCentral()
//        google()
//        gradlePluginPortal()
//        maven { url = uri("https://oss.sonatype.org/service/local/repositories/snapshots/content") } // SNAPSHOT Versions for statik
//    }
//}
//
//plugins {
//    id("com.squareup.invert")
//}
//
//invert {
//    ownershipCollector(com.squareup.invert.GitHubCodeOwnersInvertOwnershipCollector)
//    addStatCollector(com.squareup.invert.RealAnvilContributesBindingStatCollector())
//    addStatCollector(com.squareup.invert.LinesOfCodeStatCollector())
//}
//
//
