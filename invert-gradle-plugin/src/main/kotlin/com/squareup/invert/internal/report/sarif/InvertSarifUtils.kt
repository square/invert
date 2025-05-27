package com.squareup.invert.internal.report.sarif

import com.squareup.invert.internal.report.sarif.InvertSarifReportWriter.Companion.createSarifSchema
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerInfo
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
import io.github.detekt.sarif4k.SarifSchema210
import org.jetbrains.annotations.VisibleForTesting
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.plus


data object SarifKey {
    const val OWNER = "owner"
    const val MODULE = "module"
    const val UNIQUE_ID = "uniqueId"
    const val DESCRIPTION = "description"
    const val FILE_TYPE = "fileType"
    const val EXTRAS = "extras"
    const val CATEGORIES = "categories"
    const val TITLE = "title"
}

/**
 * Extension function to convert Stat to SARIF results.
 */
@VisibleForTesting
fun Stat.asSarifResult(
    module: ModulePath,
    key: StatKey
): List<SarifResult> = when (this) {
    is Stat.CodeReferencesStat -> value.map {
        it.toSarifResult(
            key = key, modulePath = module
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
    modulePath: ModulePath?
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
                    value = extras + mapOf(
                        SarifKey.FILE_TYPE to filePath.split(".").last()
                    )
                )
            )
        )
    ),
    properties = PropertyBag(
        value = mapOf(
            SarifKey.OWNER to (owner ?: OwnerInfo.UNOWNED),
            SarifKey.MODULE to modulePath,
            SarifKey.UNIQUE_ID to uniqueId,
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
            val results = rulesAndResults.getOrPut(key = rule) { mutableListOf() }

            // Add results for this stat
            results.addAll(
                elements = stat.asSarifResult(
                    module = modulePath,
                    key = statKey
                )
            )
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
            value = mapOf(
                SarifKey.DESCRIPTION to description,
                SarifKey.EXTRAS to extras,
                SarifKey.CATEGORIES to category,
                SarifKey.TITLE to title
            )
        )
    )

@VisibleForTesting
fun createSarifSchemaFromResults(
    rule: ReportingDescriptor,
    results: List<SarifResult>
): SarifSchema210 {
    return createSarifSchema(rule = listOf(rule), results = results)
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