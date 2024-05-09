import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("java-gradle-plugin")
    alias(libs.plugins.vanniktech.maven.publish)
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

mavenPublishing {
    pomFromGradleProperties()
    signAllPublications()
    publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
}

// Allow overwriting of HTML & JS files in resources/META-INF during publishing
tasks {
    named<Copy>("processResources") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}


// ---------
val invertPluginProject = project
val invertReportProject = rootProject.project(":invert-report")
val invertReportCommonProject = rootProject.project(":invert-report-common")

val pluginProcessResourcesTask = invertPluginProject.tasks.named("processResources")

val copyInvertJsFilesTask = invertReportProject.tasks.register<Copy>("copyInvertJs") {
    val rootProjectDir = rootProject.layout.projectDirectory.asFile
    val sourceDir = File(rootProjectDir, "invert-report/build/dist/js/invert_web")
    val destDir = File(rootProjectDir, "invert-plugin/src/main/resources/META-INF")
    from(sourceDir) {
        include("*.js", "*.css", "*.html")
    }
    into(destDir)

    outputs.upToDateWhen { false }

    finalizedBy(pluginProcessResourcesTask)
}


invertReportProject.afterEvaluate {
    val reportWebpackTask = invertReportProject.tasks.named("jsBrowserDevelopmentWebpack")
    copyInvertJsFilesTask.configure {
        dependsOn(reportWebpackTask)
    }
}
