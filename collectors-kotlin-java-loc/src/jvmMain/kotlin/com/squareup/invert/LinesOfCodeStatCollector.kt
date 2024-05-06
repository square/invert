package com.squareup.invert

import com.squareup.invert.models.CollectedStatType
import com.squareup.invert.models.Stat.StringStat
import com.squareup.invert.models.StatInfo
import java.io.File

class LinesOfCodeStatCollector : StatCollector.GenericStatCollector {
    override fun collect(srcFolder: File, projectPath: String, kotlinSourceFiles: List<File>): StringStat? {

        return if (kotlinSourceFiles.isNotEmpty()) {
            StringStat(buildString {
                appendLine("File Count: " + kotlinSourceFiles.size)

                var kotlinLoc = 0
                kotlinSourceFiles
                    .filter { it.isFile && it.extension == "kt" }
                    .map { it.readLines().filter { line -> line.isNotBlank() } }
                    .forEach {
                        kotlinLoc += it.size
                    }
                appendLine("Lines of Code: $kotlinLoc")
            })
        } else {
            null
        }
    }

    override val statInfo: StatInfo = StatInfo(
        name = "KotlinLinesOfCode",
        description = "Kotlin Lines of Code",
        statType = CollectedStatType.GENERIC
    )

    override fun getName(): String {
        return this::class.java.name
    }
}