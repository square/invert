package com.squareup.invert

import com.squareup.invert.models.CollectedStatType
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatMetadata
import java.io.File

class LinesOfCodeStatCollector : StatCollector {
    override fun collect(
        rootProjectFolder: File,
        projectPath: String,
        kotlinSourceFiles: List<File>
    ): List<CollectedStat>? {

        return if (kotlinSourceFiles.isNotEmpty()) {
            var kotlinLoc = 0
            kotlinSourceFiles
                .filter { it.isFile && it.extension == "kt" }
                .map { it.readLines().filter { line -> line.isNotBlank() } }
                .forEach {
                    kotlinLoc += it.size
                }

            listOf(
                CollectedStat(
                    metadata = KOTLIN_FILE_COUNT,
                    stat = Stat.NumericStat(
                        value = kotlinSourceFiles.size,
                    )
                ),
                CollectedStat(
                    metadata = KOTLIN_LINES_OF_CODE,
                    stat = Stat.NumericStat(
                        value = kotlinLoc,
                    )
                )
            )
        } else {
            null
        }
    }

    private val KOTLIN_FILE_COUNT: StatMetadata = StatMetadata(
        key = "file_count_kotlin",
        description = "Kotlin File Count",
        statType = CollectedStatType.NUMERIC
    )

    private val KOTLIN_LINES_OF_CODE: StatMetadata = StatMetadata(
        key = "lines_of_code_kotlin",
        description = "Kotlin Lines of Code",
        statType = CollectedStatType.NUMERIC
    )

    override fun getName(): String {
        return this::class.java.name
    }
}