package com.squareup.invert.suppress

import com.squareup.invert.StatCollector
import com.squareup.invert.models.CollectedStatType
import com.squareup.invert.models.Stat.StringStat
import com.squareup.invert.models.StatInfo
import com.squareup.psi.toKtFile
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import java.io.File

/**
 * This collector invokes the [FindAnvilContributesBinding] class collect all instances
 * of the Anvil ContributesBinding annotation, and puts it into a Stat that can be used collected
 * by the [InvertGradlePlugin]
 */
class SupressionsStatCollector : StatCollector.GenericStatCollector {
    override fun collect(srcFolder: File, projectPath: String, kotlinSourceFiles: List<File>): StringStat? {
        val allSuppressions = mutableListOf<Suppression>()
        kotlinSourceFiles.forEach { file ->
            if (file.exists()) {
                val ktFile = file.toKtFile()
                val relativePath = file.absolutePath.replace(srcFolder.absolutePath, "")

                val suppressionsVisitor = SuppressionsVisitor()
                PsiTreeUtil.findChildrenOfType(ktFile, KtAnnotationEntry::class.java).forEach {
                    suppressionsVisitor.processAnnotationEntry(ktFile, it, relativePath)
                }
                allSuppressions.addAll(suppressionsVisitor.suppressed)
            }
        }

        return if (allSuppressions.isNotEmpty()) {
            StringStat(
                buildString {
                    val suppressionsByType = allSuppressions
                        .groupBy { it.type }
                    suppressionsByType.keys.sorted().forEach { type ->
                        appendLine("@Suppress(\"$type\")")
                        val bindings = suppressionsByType[type]
                        bindings?.map { "${it.filePath}:${it.startLine}:${it.startColumn}" }?.sorted()
                            ?.forEach { appendLine("* $it") }
                        appendLine()
                    }
                }
            )
        } else {
            null
        }
    }

    override val statInfo: StatInfo = StatInfo(
        name = "SuppressAnnotationUsages",
        description = "@Suppress Annotation Usages",
        statType = CollectedStatType.STRING
    )

    override fun getName(): String {
        return this::class.java.name
    }
}