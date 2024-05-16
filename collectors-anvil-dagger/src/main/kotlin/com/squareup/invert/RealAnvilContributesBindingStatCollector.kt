package com.squareup.invert

import com.squareup.invert.models.CollectedStatType
import com.squareup.invert.models.Stat.StringStat
import com.squareup.invert.models.StatInfo
import java.io.File

/**
 * This collector invokes the [FindAnvilContributesBinding] class collect all instances
 * of the Anvil ContributesBinding annotation, and puts it into a Stat that can be used collected
 * by the [InvertGradlePlugin]
 */
class RealAnvilContributesBindingStatCollector : StatCollector.GenericStatCollector {
    override fun collect(srcFolder: File, projectPath: String, kotlinSourceFiles: List<File>): StringStat? {
        val findAnvil = FindAnvilContributesBinding()
        kotlinSourceFiles.forEach { kotlinFile ->
            findAnvil.handleKotlinFile(kotlinFile)
        }
        val bindings = findAnvil.getCollectedContributesBindings()
        return if (bindings.isNotEmpty()) {
            StringStat(
                buildString {
//                    val bindingsByScope = bindings.groupBy { it.scope }
//                    bindingsByScope.keys.sorted().forEach { scope ->
//                        appendLine("SCOPE: " + scope)
//                        val bindings = bindingsByScope[scope]
//                        bindings?.map { "${it.boundType} ➡️ ${it.boundImplementation}" }?.sorted()
//                            ?.forEach { appendLine(it) }
//                    }
                    findAnvil.getCollectedContributionsAndConsumptions().forEach { injections ->
                        appendLine("---- Class ${injections.classFqName} in file: ${injections.fileName} at line ${injections.lineNumber} ----")
                        if (injections.contributions.isNotEmpty()) {
                            injections.contributions.forEach {
                                appendLine("Provides:")
                                appendLine("* ${it.boundType} ➡\uFE0F ${it.boundImplementation} for Scope ➡\uFE0F ${it.scope}")
                            }
                        }
                        if (injections.consumptions.isNotEmpty()) {
                            appendLine("Injects:")
                            injections.consumptions.forEach {
                                appendLine("* ${it.qualifierAnnotations.joinToString(" ")} ${it.type}")
                            }
                        }
                        appendLine()
//                        val contributionsByScope = injections.contributions.groupBy { it.scope }
//                        appendLine(contributionsByScope.toString())
//                        contributionsByScope.keys.sorted().forEach { scope ->
//                            appendLine("SCOPE: " + scope)
//                            val contributionsForScope = contributionsByScope[scope]
//                            contributionsForScope?.map { "${it.boundType} ➡️ ${it.boundImplementation}" }?.sorted()
//                                ?.forEach { appendLine(it) }
//                        }
//                        appendLine(injections.toString().replace(",", "\n").replace("(", "\n"))
                    }
                }

            )
        } else {
            null
        }
    }

    override val statInfo: StatInfo = StatInfo(
        name = "AnvilContributesBinding",
        description = "Anvil ContributesBinding Annotation Information",
        statType = CollectedStatType.STRING
    )

    override fun getName(): String {
        return this::class.java.name
    }
}