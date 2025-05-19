package com.squareup.invert.internal.report.sarif

import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.internal.report.json.InvertJsonReportWriter.Companion.writeJsonFile
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.HistoricalData
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.StatTotalAndMetadata
import com.squareup.invert.models.js.StatsJsReportModel
import io.github.detekt.sarif4k.ArtifactLocation
import io.github.detekt.sarif4k.Location
import io.github.detekt.sarif4k.Message
import io.github.detekt.sarif4k.MultiformatMessageString
import io.github.detekt.sarif4k.PhysicalLocation
import io.github.detekt.sarif4k.PropertyBag
import io.github.detekt.sarif4k.Region
import io.github.detekt.sarif4k.ReportingDescriptor
import io.github.detekt.sarif4k.Result as SarifResult
import io.github.detekt.sarif4k.Run
import io.github.detekt.sarif4k.SarifSchema210
import io.github.detekt.sarif4k.SarifSerializer
import io.github.detekt.sarif4k.Tool
import io.github.detekt.sarif4k.ToolComponent
import io.github.detekt.sarif4k.Version
import kotlinx.serialization.KSerializer
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
     * Extension function to convert StatsJsReportModel to a map of SARIF rules to their results.
     */
    private fun StatsJsReportModel.asSarifRulesAndResults(): Map<ReportingDescriptor, List<SarifResult>> {
        val rulesAndResults = mutableMapOf<ReportingDescriptor, MutableList<SarifResult>>()

        statsByModule.forEach { (modulePath, statMap) ->
            statMap.forEach { (statKey, stat) ->
                // Get or create the rule for this stat
                val rule = statInfos[statKey]?.asReportingDescriptor() ?: return@forEach
                
                // Get or create the results list for this rule
                val results = rulesAndResults.getOrPut(rule) { mutableListOf() }
                
                // Add results for this stat
                results.addAll(stat.asSarifResult(modulePath, statKey, statInfos[statKey]))
            }
        }

        return rulesAndResults
    }

    /**
     * Creates a SARIF report for Invert statistics and metadata.
     *
     * @param allConfigurationsData Configuration data for all projects
     * @param allProjectsDependencyData Dependency data for all projects
     * @param allProjectsStatsData Statistics data for all projects
     * @param allPluginsData Plugin data for all projects
     * @param allOwnersData Ownership data for all projects
     * @param globalStats Global statistics with metadata
     * @param reportMetadata Report metadata
     * @param historicalData Historical data for comparison
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

    /**
     * Writes a JSON file to the specified directory.
     */
    private fun <T> writeJsonFileInDir(
        jsonFileKey: InvertPluginFileKey,
        serializer: KSerializer<T>,
        value: T,
    ) = writeJsonFile(
        logger = logger,
        jsonFileKey = jsonFileKey,
        jsonOutputFile = InvertFileUtils.outputFile(
            directory = rootBuildSarifReportsDir,
            filename = jsonFileKey.filename
        ),
        serializer = serializer,
        value = value
    )

    /**
     * Creates a SARIF result for numeric stats.
     */
    private fun createNumericSarifResult(key: StatKey, module: ModulePath, details: String?, value: Int): SarifResult =
        SarifResult(
            ruleID = key,
            message = Message(text = "details: $details, value: $value"),
            locations = listOf(createLocation(module))
        )

    /**
     * Creates a SARIF result for string stats.
     */
    private fun createStringSarifResult(key: StatKey, module: ModulePath, details: String?, value: String): SarifResult =
        SarifResult(
            ruleID = key,
            message = Message(text = key),
            locations = listOf(createLocation(module))
        )

    /**
     * Creates a SARIF result for boolean stats.
     */
    private fun createBooleanSarifResult(key: StatKey, module: ModulePath, details: String?, value: Boolean): SarifResult =
        SarifResult(
            ruleID = key,
            message = Message(text = key),
            locations = listOf(createLocation(module))
        )

    /**
     * Creates a SARIF location for a module.
     */
    private fun createLocation(module: ModulePath): Location =
        Location(
            physicalLocation = PhysicalLocation(
                artifactLocation = ArtifactLocation(uri = module)
            )
        )



    companion object {
        private const val SARIF_FILE_NAME = "invert-report.sarif"

        fun writeToSarifReport(
            values: List<Stat.CodeReferencesStat.CodeReference>,
            metadata: StatMetadata,
            fileName: File,
            description: String
        ) {
            val results = values.map { it.toSarifResult(metadata.key, modulePath = null, metadata) }
            val rule = metadata.asReportingDescriptor()
            val sarifSchema = writeSarifSchema(rule = rule, results = results)
            val sarifJson = SarifSerializer.toMinifiedJson(sarifSchema)
            fileName.writeText(sarifJson)
        }
    }
}

/**
 * Extension function to convert Stat to SARIF results.
 */
private fun Stat.asSarifResult(
    module: ModulePath,
    key: StatKey,
    metadata: StatMetadata?
): List<SarifResult> = when (this) {
    is Stat.CodeReferencesStat -> value.map {
        it.toSarifResult(
            key = key, modulePath = module, metadata = metadata
        )
    }
    else -> emptyList()

//    is Stat.NumericStat -> listOf(createNumericSarifResult(key, module, details, value))
//    is Stat.StringStat -> listOf(createStringSarifResult(key, module, details, value))
//    is Stat.BooleanStat -> listOf(createBooleanSarifResult(key, module, details, value))
}

/**
 * Extension function to convert CodeReference to SARIF result.
 */
private fun Stat.CodeReferencesStat.CodeReference.toSarifResult(
    key: StatKey,
    modulePath: ModulePath?,
    metadata: StatMetadata?
): SarifResult = SarifResult(
    ruleID = key,
    message = Message(text = code),
    locations = listOf(
        Location(
            physicalLocation = PhysicalLocation(
                artifactLocation = ArtifactLocation(uri = filePath),
                region = Region(
                    startLine = startLine.toLong(),
                    endLine = endLine.toLong(),
                    sourceLanguage = code?.trim()
                ),
                properties = PropertyBag(
                    extras + mapOf(
                        "fileType" to filePath.split(".").last()
                    )
                )
            )
        )
    ),
    properties = PropertyBag(
        mapOf(
            "owner" to (owner ?: "Unknown"),
            "module" to modulePath,
            "uniqueId" to uniqueId,
        )
    )
)

/**
 * Extension function to convert StatMetadata to SARIF reporting descriptor.
 */
private fun StatMetadata.asReportingDescriptor(): ReportingDescriptor =
    ReportingDescriptor(
        id = key,
        name = this.title,
        fullDescription = MultiformatMessageString(markdown = description, text = description),
        properties = PropertyBag(
            mapOf(
                "description" to description,
                "extras" to extras,
                "category" to category,
                "title" to title
            )
        )
    )

private fun writeSarifSchema(
    rule: ReportingDescriptor,
    results: List<SarifResult>
): SarifSchema210 {
    return createSarifSchema(toolName = rule.id, toolVersion = "1.0.0", rule = listOf(rule), results = results)
}

private fun createSarifSchema(
    toolName: String = "Invert",
    toolVersion: String = "1.0.0",
    rule: List<ReportingDescriptor>,
    results: List<SarifResult>
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