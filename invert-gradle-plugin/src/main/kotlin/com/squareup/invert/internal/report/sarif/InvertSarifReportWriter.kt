package com.squareup.invert.internal.report.sarif

import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.StatsJsReportModel
import io.github.detekt.sarif4k.ReportingDescriptor
import io.github.detekt.sarif4k.Result as SarifResult
import io.github.detekt.sarif4k.Run
import io.github.detekt.sarif4k.SarifSchema210
import io.github.detekt.sarif4k.SarifSerializer
import io.github.detekt.sarif4k.Tool
import io.github.detekt.sarif4k.ToolComponent
import io.github.detekt.sarif4k.Version
import java.io.File
import java.nio.file.Files

/**
 * Writer for generating SARIF (Static Analysis Results Interchange Format) reports for Invert.
 * SARIF is a standard format for static analysis tools to report their results.
 */
class InvertSarifReportWriter(
    private val logger: InvertLogger,
    rootBuildReportsDir: File,
) {
    private val rootBuildSarifReportsDir = File(rootBuildReportsDir, InvertFileUtils.SARIF_FOLDER_NAME)

    /**
     * Writes a SARIF report to a file.
     */
    private fun writeToSarif(
        jsonFileKey: InvertPluginFileKey,
        rulesAndResults: Map<ReportingDescriptor, List<SarifResult>>
    ) {
        val sarif = createSarifSchema(
            toolName = "Invert",
            toolVersion = "1.0.0",
            rule = rulesAndResults.keys.toList(),
            results = rulesAndResults.values.flatten()
        )


        val sarifFile = InvertFileUtils.outputFile(
            directory = rootBuildSarifReportsDir,
            filename = jsonFileKey.filename
        )

        Files.write(sarifFile.toPath(), SarifSerializer.toMinifiedJson(sarif).toByteArray())
    }

    /**
     * Creates a SARIF report for Invert statistics and metadata.
     *
     * @param allProjectsStatsData Statistics data for all projects
     */
    fun createInvertSarifReport(
        allProjectsStatsData: StatsJsReportModel
    ) {
        val rulesAndResults = allProjectsStatsData.asSarifRulesAndResults()
        writeToSarif(
            jsonFileKey = InvertPluginFileKey.STATS_SARIF,
            rulesAndResults = rulesAndResults
        )
    }

    companion object {
        private const val SARIF_FILE_NAME = "invert-report.sarif"

        fun writeToSarifReport(
            values: List<Stat.CodeReferencesStat.CodeReference>,
            metadata: StatMetadata,
            fileName: File,
            description: String
        ) {
            if (!fileName.exists()) {
                fileName.parentFile.mkdirs()
                fileName.createNewFile()
            }

            val results = values.map { it.toSarifResult(metadata.key, modulePath = null) }
            val rule = metadata.asReportingDescriptor(shortDescription = description)
            val sarifSchema = createSarifSchemaFromResults(rule = rule, results = results)
            val sarifJson = SarifSerializer.toMinifiedJson(sarifSchema)
            fileName.writeText(sarifJson)
        }

        fun createSarifSchema(
            rule: List<ReportingDescriptor>,
            results: List<SarifResult>,
            toolName: String = "Invert",
            toolVersion: String = "1.0.0",
        ): SarifSchema210 {
            return SarifSchema210(
                version = Version.The210,
                schema = "https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0.json",
                runs = listOf(
                    Run(
                        tool = Tool(
                            driver = ToolComponent(
                                name = toolName,
                                version = toolVersion,
                                rules = rule
                            )
                        ),
                        results = results
                    )
                )
            )
        }
    }
}