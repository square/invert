package com.squareup.invert

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

class SamsRule(val config: Config) : Rule(config) {

    override val issue = Issue(
        javaClass.simpleName,
        Severity.CodeSmell,
        "This rule reports a file with an excessive function count.",
        Debt.TWENTY_MINS
    )

    private val threshold = 10
    private var amount: Int = 0

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        println("TooManyFunctions::visitKtFile ${file}")
        report(
            CodeSmell(
                issue, Entity.from(file),
                "Sam's Code Smell"
            )
        )

        report(
            ThresholdedCodeSmell(
                issue,
                entity = Entity.from(file),
                metric = Metric(type = "SIZE", value = amount, threshold = threshold),
                message = "visitKtFile The file ${file.name} has $amount function declarations. " +
                        "Threshold is specified with $threshold.",
                references = emptyList()
            )
        )
        amount = 0
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        println("TooManyFunctions::visitNamedFunction ${function}")
        amount++
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(function),
                message = "visitNamedFunction"
            )
        )
    }
}