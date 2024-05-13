plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.squareup.anvil") version "2.4.9"
    id("io.gitlab.arturbosch.detekt") version ("1.23.3")
}

detekt {
//    allRules = true
//    debug = true

    reports {
        xml.required = false
        html.required = true
        txt.required = false
        sarif.required = false
        md.required = false
        custom {
            // The simple class name of your custom report.
            reportId = "sam"
            val samsReportFile = file("build/reports/detekt/sam.txt")
            println("samsReportFile $samsReportFile")
            outputLocation.set(samsReportFile)
        }
    }
}

dependencies {
    implementation(project(":scopes"))
    implementation(libs.kotlinx.serialization.json)
    implementation("com.squareup.anvil:annotations:2.4.9")
    implementation("javax.inject:javax.inject:1")
    detektPlugins(project(":custom-detekt-rules"))
}
