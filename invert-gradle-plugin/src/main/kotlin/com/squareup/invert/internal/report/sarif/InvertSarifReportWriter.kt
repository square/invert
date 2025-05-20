package com.squareup.invert.internal.report.sarif

import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.internal.report.json.InvertJsonReportWriter.Companion.writeJsonFile
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.StatsJsReportModel
import io.github.detekt.sarif4k.ArtifactLocation
import io.github.detekt.sarif4k.Location
import io.github.detekt.sarif4k.Message
import io.github.detekt.sarif4k.MultiformatMessageString
import io.github.detekt.sarif4k.PhysicalLocation
import io.github.detekt.sarif4k.PropertyBag
import io.github.detekt.sarif4k.Region
import io.github.detekt.sarif4k.ReportingDescriptor
import io.github.detekt.sarif4k.ReportingDescriptorReference
import io.github.detekt.sarif4k.Result as SarifResult
import io.github.detekt.sarif4k.Run
import io.github.detekt.sarif4k.SarifSchema210
import io.github.detekt.sarif4k.SarifSerializer
import io.github.detekt.sarif4k.Tool
import io.github.detekt.sarif4k.ToolComponent
import io.github.detekt.sarif4k.Version
import kotlinx.serialization.KSerializer
import org.jetbrains.annotations.VisibleForTesting
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

            val results = values.map { it.toSarifResult(metadata.key, modulePath = null, metadata) }
            val rule = metadata.asReportingDescriptor(shortDescription = description)
            val sarifSchema = createSarifSchemaFromResults(rule = rule, results = results)
            val sarifJson = SarifSerializer.toMinifiedJson(sarifSchema)
            fileName.writeText(sarifJson)
        }
    }
}

/**
 * Extension function to convert Stat to SARIF results.
 */
@VisibleForTesting
fun Stat.asSarifResult(
    module: ModulePath,
    key: StatKey,
    metadata: StatMetadata?
): List<SarifResult> = when (this) {
    is Stat.CodeReferencesStat -> value.map {
        it.toSarifResult(
            key = key, modulePath = module, metadata = metadata
        )
    }
    // No support for other stat types in SARIF
    else -> emptyList()
}

/**
 * Extension function to convert CodeReference to SARIF result.
 */
@VisibleForTesting
fun Stat.CodeReferencesStat.CodeReference.toSarifResult(
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
                    sourceLanguage = determineSourceLanguage(filePath)
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
    ),
    rule = ReportingDescriptorReference(id = key)
)

/**
 * Extension function to convert StatsJsReportModel to a map of SARIF rules to their results.
 */
@VisibleForTesting
fun StatsJsReportModel.asSarifRulesAndResults(): Map<ReportingDescriptor, List<SarifResult>> {
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
 * Extension function to convert StatMetadata to SARIF reporting descriptor.
 */
@VisibleForTesting
fun StatMetadata.asReportingDescriptor(shortDescription: String = ""): ReportingDescriptor =
    ReportingDescriptor(
        id = key,
        name = this.title,
        fullDescription = MultiformatMessageString(markdown = description, text = description),
        shortDescription = MultiformatMessageString(text = shortDescription),
        properties = PropertyBag(
            mapOf(
                "description" to description,
                "extras" to extras,
                "category" to category,
                "title" to title
            )
        )
    )

@VisibleForTesting
fun createSarifSchemaFromResults(
    rule: ReportingDescriptor,
    results: List<SarifResult>
): SarifSchema210 {
    return createSarifSchema(toolName = rule.id, rule = listOf(rule), results = results)
}

private fun createSarifSchema(
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

/**
 * Simple function to determine the source language based on the file path.
 */
private fun determineSourceLanguage(filePath: String): String {
    return when {
        filePath.endsWith(".kt") -> "kotlin"
        filePath.endsWith(".java") -> "java"
        filePath.endsWith(".js") -> "javascript"
        filePath.endsWith(".ts") -> "typescript"
        filePath.endsWith(".swift") -> "swift"
        filePath.endsWith(".m") -> "objective-c"
        filePath.endsWith(".mm") -> "objective-c++"
        filePath.endsWith(".c") -> "c"
        filePath.endsWith(".cpp") -> "c++"
        filePath.endsWith(".h") -> "c-header"
        filePath.endsWith(".hpp") -> "c++-header"
        filePath.endsWith(".py") -> "python"
        filePath.endsWith(".rb") -> "ruby"
        filePath.endsWith(".go") -> "go"
        filePath.endsWith(".php") -> "php"
        filePath.endsWith(".html") -> "html"
        filePath.endsWith(".css") -> "css"
        filePath.endsWith(".xml") -> "xml"
        filePath.endsWith(".json") -> "json"
        filePath.endsWith(".yaml") || filePath.endsWith(".yml") -> "yaml"
        else -> "unknown"
    }
}