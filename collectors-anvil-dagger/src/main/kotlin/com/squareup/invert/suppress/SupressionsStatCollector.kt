package com.squareup.invert.suppress

import com.squareup.invert.CollectedStat
import com.squareup.invert.StatCollector
import com.squareup.invert.models.CollectedStatType
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatMetadata
import com.squareup.psi.toKtFile
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import java.io.File

/**
 * This collector invokes the [FindAnvilContributesBinding] class collect all instances
 * of the Anvil ContributesBinding annotation, and puts it into a Stat that can be used collected
 * by the [InvertGradlePlugin]
 */
class SupressionsStatCollector : StatCollector {
    override fun collect(
        rootProjectFolder: File,
        projectPath: String,
        kotlinSourceFiles: List<File>
    ): List<CollectedStat>? {
        val allSuppressions = mutableListOf<Suppression>()
        kotlinSourceFiles.forEach { file ->
            if (file.exists()) {
                val ktFile = file.toKtFile()
                val relativePath = file.absolutePath.replace(rootProjectFolder.absolutePath, "")

                val suppressionsVisitor = SuppressionsVisitor()
                PsiTreeUtil.findChildrenOfType(ktFile, KtAnnotationEntry::class.java).forEach {
                    suppressionsVisitor.processAnnotationEntry(ktFile, it, relativePath)
                }
                allSuppressions.addAll(suppressionsVisitor.suppressed)
            }
        }

        val collectedStats = mutableListOf<CollectedStat>()

        if (allSuppressions.isNotEmpty()) {
            val suppressionsByTypeMap = allSuppressions
                .groupBy { it.type }

            suppressionsByTypeMap.keys.sorted().map { type ->
                suppressionsByTypeMap[type]?.let { suppressionsByTypeList ->
                    val numericStat = suppressionsByTypeList.let {
                        Stat.NumericStat(
                            value = suppressionsByTypeList.size,
                            details = buildString {
                                suppressionsByTypeList
                                    .map { "${it.filePath}:${it.startLine}:${it.startColumn}" }
                                    .sorted()
                                    .forEach { appendLine("* $it") }
                            }
                        )
                    }
                    collectedStats.add(
                        CollectedStat(
                            metadata = StatMetadata(
                                key = "suppress_annotation_$type",
                                description = "@Suppress(\"$type\")",
                                statType = CollectedStatType.NUMERIC,
                                category = "suppress_annotation"
                            ),
                            stat = numericStat
                        )
                    )
                }
            }
        }
        return collectedStats.ifEmpty {
            null
        }
    }

    override fun getName(): String {
        return this::class.java.name
    }
}