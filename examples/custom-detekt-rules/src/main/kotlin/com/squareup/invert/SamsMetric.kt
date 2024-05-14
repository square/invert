package com.squareup.invert

import io.github.detekt.metrics.processors.AbstractProjectMetricProcessor
import io.gitlab.arturbosch.detekt.api.DetektVisitor
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.psi.KtFile

class SamsMetric : AbstractProjectMetricProcessor() {

    companion object {
        val numberOfSamsKey = Key<Int>("number of kotlin files sam")
    }

    override val id: String = this::class.java.simpleName
    override val visitor = SamCountVisitor()
    override val key = numberOfSamsKey

    class SamCountVisitor : DetektVisitor() {
        override fun visitKtFile(file: KtFile) {
            file.putUserData(numberOfSamsKey, 1)
        }
    }
}