package com.squareup.invert.collectors.contains

import com.squareup.invert.CollectedStat
import com.squareup.invert.InvertCollectContext
import com.squareup.invert.StatCollector
import com.squareup.invert.collectors.internal.wrapCodeForMarkdown
import com.squareup.invert.models.ExtraDataType
import com.squareup.invert.models.ExtraMetadata
import com.squareup.invert.models.Markdown
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatMetadata
import java.io.File

open class InvertContainsStatCollector(
  private val statKey: String,
  private val statTitle: String,
  private val statDescription: Markdown? = null,
  private val linePredicate: (String) -> Boolean,
  private val filePredicate: (File) -> Boolean = { true },
) : StatCollector {
  override fun collect(
    invertCollectContext: InvertCollectContext,
  ): List<CollectedStat>? {
    val codeReferences = mutableListOf<Stat.CodeReferencesStat.CodeReference>()
    invertCollectContext.moduleDir
      .walkTopDown()
      .filter { it.isFile && it.length() > 0 }
      .filter { filePredicate(it) }
      .forEach { sourceFile ->
        val relativeFilePath = sourceFile.relativeTo(invertCollectContext.gitCloneDir).path
        sourceFile.readLines()
          .map { it.trim() }
          .forEachIndexed { index, line ->
            if (linePredicate(line)) {
              codeReferences.add(
                Stat.CodeReferencesStat.CodeReference(
                  filePath = relativeFilePath,
                  startLine = index + 1,
                  endLine = index + 1,
                  code = line.wrapCodeForMarkdown(),
                )
              )
            }
          }
      }

    return if (codeReferences.isNotEmpty()) {
      listOf(
        CollectedStat(
          metadata = StatMetadata(
            key = statKey,
            title = statTitle,
            description = statDescription ?: statTitle,
            dataType = StatDataType.CODE_REFERENCES,
            extras = listOf(
              ExtraMetadata(
                key = "filePredicate",
                type = ExtraDataType.STRING,
                description = "Predicate to filter files for this stat collector"
              )
            )
          ),
          stat = Stat.CodeReferencesStat(codeReferences)
        )
      )
    } else {
      null
    }
  }

  override fun getName(): String {
    return this::class.java.name + "-" + statKey
  }
}