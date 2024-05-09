plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

kotlin {
    js {
        binaries.executable()
        val pluginResourcesDir =
            rootProject.layout.projectDirectory.file("invert-gradle-plugin/src/main/resources/META-INF").asFile
        browser {
            runTask {
                mainOutputFileName.set("invert_web.js")
            }
            webpackTask {
                mainOutputFileName.set("invert_web.js")
            }

            commonWebpackConfig {
                outputPath = pluginResourcesDir
            }
        }
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":invert-report-common"))
                implementation(libs.kotlinx.serialization.json)
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.9.1")
                implementation(compose.html.core)
                implementation(compose.runtime)
            }
        }
        jsMain.resources.srcDir(File(rootProject.rootDir, "invert-gradle-plugin/src/main/resources/META-INF"))
    }
}

compose {
    web {
    }
}

project(":invert-report") {
    val isCI = System.getenv().containsKey("GITHUB_ACTIONS")
    val webpackTaskName = if (isCI) {
        "jsBrowserProductionWebpack"
    } else {
        "jsBrowserDevelopmentWebpack"
    }
    val reportWebpackTask = tasks.named(webpackTaskName)
    project(":invert-gradle-plugin").tasks.named("processResources").configure {
        dependsOn(reportWebpackTask)
    }
}