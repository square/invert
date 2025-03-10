package com.squareup.invert.collectors.linesofcode

import com.squareup.invert.CollectedStat
import com.squareup.invert.InvertCollectContext
import com.squareup.invert.StatCollector
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.projectSrcDir
import java.io.File

open class LinesOfCodeStatCollector(
  name: String,
  private val fileExtensions: List<String>,
  keySuffix: String = fileExtensions.joinToString("_"),
  private val sourcesDirectory: (InvertCollectContext) -> File = {
    it.projectSrcDir
  },
) : StatCollector {

  companion object {
    const val STAT_CATEGORY_LINES_OF_CODE = "Lines of Code"
  }

  private val fileCountStatMetadata: StatMetadata = StatMetadata(
    key = "file_count_$keySuffix",
    title = "File Count - $name",
    dataType = StatDataType.NUMERIC,
    category = STAT_CATEGORY_LINES_OF_CODE,
  )

  private val linesOfCodeStatMetadata: StatMetadata = StatMetadata(
    key = "lines_of_code_$keySuffix",
    title = "Lines of Code - $name",
    dataType = StatDataType.NUMERIC,
    category = STAT_CATEGORY_LINES_OF_CODE,
  )

  override fun collect(
    invertCollectContext: InvertCollectContext,
  ): List<CollectedStat>? {
    val matchingSourceFiles = sourcesDirectory(invertCollectContext)
      .walkTopDown()
      .filter { file -> file.isFile }
      .filter { fileExtensions.contains(it.extension) }
      .toList()

    return if (matchingSourceFiles.isNotEmpty()) {
      var totalLoc = 0
      matchingSourceFiles
        .map { it.readLines().filter { line -> line.isNotBlank() } }
        .forEach {
          totalLoc += it.size
        }

      listOf(
        CollectedStat(
          metadata = fileCountStatMetadata,
          stat = Stat.NumericStat(
            value = matchingSourceFiles.size,
          )
        ),
        CollectedStat(
          metadata = linesOfCodeStatMetadata,
          stat = Stat.NumericStat(
            value = totalLoc,
          )
        )
      )
    } else {
      null
    }
  }

  override fun getName(): String {
    return this::class.java.name
  }
}
