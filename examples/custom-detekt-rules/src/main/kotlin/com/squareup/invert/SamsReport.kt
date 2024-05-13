package com.squareup.invert

import io.gitlab.arturbosch.detekt.api.*

class SamsReport : OutputReport() {

    override val id: String = "sam"
    override val ending: String = "txt"

    private lateinit var config: Config

    @UnstableApi
    override fun init(context: SetupContext) {
        super.init(context)

        println(this::class.java.simpleName + "::init()")
//        this.basePath = context.getOrNull<Path>(DETEKT_OUTPUT_REPORT_BASE_PATH_KEY)
//            ?.absolute()
//            ?.invariantSeparatorsPathString
//            ?.let {
//                if (!it.endsWith("/")) "$it/" else it
//            }
//
        this.config = context.config
    }

    override fun render(detektion: Detektion): String {
        println("SamsReport::render()")
        return buildString {
            appendLine("Sams Report")
            appendLine(detektion.metrics.map { it.toString() })
        }
    }
}