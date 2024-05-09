plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

kotlin {
    js {
        binaries.executable()
        browser {
            runTask(Action {
                mainOutputFileName.set("invert_web.js")
            })
            webpackTask(Action {
                mainOutputFileName.set("invert_web.js")
            })
            distribution(Action {
                distributionName.set("invert_web")
            })
        }
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":invert-report"))
                implementation(libs.kotlinx.serialization.json)
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.9.1")
                implementation(compose.html.core)
                implementation(compose.runtime)
            }
        }
        jsMain.resources.srcDir(File(rootProject.rootDir, "invert-report-common/src/jsMain/resources"))
    }
}

compose {
    web {
    }
}
