package com.squareup.invert.internal.report.sarif

import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.OwnerInfo
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.StatsJsReportModel
import io.github.detekt.sarif4k.ReportingDescriptor
import io.github.detekt.sarif4k.Run
import io.github.detekt.sarif4k.SarifSchema210
import io.github.detekt.sarif4k.SarifSerializer
import io.github.detekt.sarif4k.Tool
import io.github.detekt.sarif4k.ToolComponent
import io.github.detekt.sarif4k.Version
import okio.buffer
import okio.sink
import java.io.File
import io.github.detekt.sarif4k.Result as SarifResult

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
            rule = rulesAndResults.keys.toList(),
            results = rulesAndResults.values.flatten()
        )

        val sarifFile = InvertFileUtils.outputFile(
            directory = rootBuildSarifReportsDir,
            filename = jsonFileKey.filename
        )

        sarifFile.sink().buffer().use { sink ->
            sink.writeUtf8(SarifSerializer.toMinifiedJson(sarif))
        }
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
        private const val SARIF_INVERT_TOOL_NAME = "invert"
        private const val SARIF_INVERT_TOOL_VERSION = "1.0.0"
        private const val SARIF_SCHEMA = "https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0.json"

        fun writeToSarifReport(
            values: List<Stat.CodeReferencesStat.CodeReference>,
            metadata: StatMetadata,
            fileName: File,
            description: String,
            moduleExtraKey: String,
            ownerExtraKey: String,
        ) {
            if (!fileName.exists()) {
                fileName.parentFile.mkdirs()
                fileName.createNewFile()
            }

            val results = values.map {
                it.toSarifResult(
                    metadata.key,
                    modulePath = it.extras.getOrElse(moduleExtraKey) { null },
                    ownerInfo = it.extras.getOrElse(ownerExtraKey) { OwnerInfo.UNOWNED }
                )
            }
            val rule = metadata.asReportingDescriptor(shortDescription = description)
            val sarifSchema = createSarifSchemaFromResults(rule = rule, results = results)
            val sarifJson = SarifSerializer.toMinifiedJson(sarifSchema)
            fileName.sink().buffer().use { sink ->
                sink.writeUtf8(sarifJson)
            }
        }

        fun createSarifSchema(
            rule: List<ReportingDescriptor>,
            results: List<SarifResult>,
            toolName: String = SARIF_INVERT_TOOL_NAME,
            toolVersion: String = SARIF_INVERT_TOOL_VERSION,
        ): SarifSchema210 {
            return SarifSchema210(
                version = Version.The210,
                schema = SARIF_SCHEMA,
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