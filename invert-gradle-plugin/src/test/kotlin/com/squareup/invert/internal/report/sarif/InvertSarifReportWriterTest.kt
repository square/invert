package com.squareup.invert.internal.report.sarif

import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.ExtraDataType
import com.squareup.invert.models.ExtraMetadata
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.StatsJsReportModel
import io.github.detekt.sarif4k.SarifSerializer
import io.github.detekt.sarif4k.fromJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.File

class InvertSarifReportWriterTest {

    private val testDir = File("build/test-reports")
    private val logger = object : InvertLogger {
        override fun lifecycle(message: String) {}
        override fun info(message: String) {}
        override fun warn(message: String) {}
    }

    @Test
    fun `test createInvertSarifReport generates correct SARIF file`() {
        // Given
        val writer = InvertSarifReportWriter(logger, testDir)
        val statsData = createTestStatsData()

        // When
        writer.createInvertSarifReport(statsData)

        // Then
        val sarifFile = File(testDir, InvertFileUtils.SARIF_FOLDER_NAME + "/" + InvertPluginFileKey.STATS_SARIF.filename)
        assertTrue(sarifFile.exists(), "SARIF file should be created")
        val sarif = sarifFile.inputStream().use { input -> SarifSerializer.fromJson(input) }
        val run = sarif.runs.single()
        assertEquals("invert", run.tool.driver.name, "Should contain tool name")
        assertEquals("1.0.0", run.tool.driver.version, "Should contain tool version")
    }

    @Test
    fun `test asSarifRulesAndResults generates correct rules and results`() {
        // Given
        val statsData = createTestStatsData()
        val writer = InvertSarifReportWriter(logger, testDir)

        // When
        val rulesAndResults = statsData.asSarifRulesAndResults()

        // Then
        assertEquals(1, rulesAndResults.size, "Should have one rule")
        val (rule, results) = rulesAndResults.entries.first()
        assertEquals("test_stat", rule.id, "Rule ID should match")
        assertEquals(2, results.size, "Should have two results")
        
        // Verify first result
        val firstResult = results[0]
        assertEquals("test_stat", firstResult.ruleID, "Result rule ID should match")
        assertEquals("test code", firstResult.message.text, "Result message should match")
        assertEquals("test.kt", firstResult.locations!![0].physicalLocation!!.artifactLocation!!.uri, "File path should match")
    }

    @Test
    fun `test writeToSarifReport generates correct SARIF file for code references`() {
        // Given
        val testFile = File(testDir, "test.sarif")
        val codeReferences = listOf(
            Stat.CodeReferencesStat.CodeReference(
                filePath = "test.kt",
                startLine = 1,
                endLine = 10,
                code = "test code",
                owner = "testOwner"
            )
        )
        val metadata = StatMetadata(
            key = "test_stat",
            description = "Test Stat",
            dataType = StatDataType.CODE_REFERENCES
        )

        // When
        InvertSarifReportWriter.writeToSarifReport(
            values = codeReferences,
            metadata = metadata,
            fileName = testFile,
            description = "Test Description",
            moduleExtraKey = "module",
            ownerExtraKey = "owner"
        )

        // Then
        assertTrue(testFile.exists(), "SARIF file should be created")
        val sarif = testFile.inputStream().use { input -> SarifSerializer.fromJson(input) }
        val run = sarif.runs.single()
        assertEquals("test_stat", run.tool.driver.rules!!.single().id, "Should contain rule ID")
        val result = run.results!!.single()
        assertEquals("test code", result.message.text, "Should contain code snippet")
        assertEquals("test.kt", result.locations!![0].physicalLocation!!.artifactLocation!!.uri, "Should contain file path")
    }


    @Test
    fun `test writeToSarifReport uses extras owner if code stat owner doesnt exist`() {
        // Given
        val testFile = File(testDir, "test.sarif")
        val codeReferences = listOf(
            Stat.CodeReferencesStat.CodeReference(
                filePath = "test.kt",
                startLine = 1,
                endLine = 10,
                code = "test code",
                owner = "codeStatOwner"
            ),
            Stat.CodeReferencesStat.CodeReference(
                filePath = "test.kt",
                startLine = 1,
                endLine = 10,
                code = "test code",
                extras =  mapOf(
                    "owner" to "testOwner",
                    "module" to "testModule"
                )
            )
        )
        val metadata = StatMetadata(
            key = "test_stat",
            description = "Test Stat",
            dataType = StatDataType.CODE_REFERENCES
        )

        // When
        InvertSarifReportWriter.writeToSarifReport(
            values = codeReferences,
            metadata = metadata,
            fileName = testFile,
            description = "Test Description",
            moduleExtraKey = "module",
            ownerExtraKey = "owner"
        )

        // Then
        assertTrue(testFile.exists(), "SARIF file should be created")
        val content = testFile.readText()
        assertTrue(content.contains("\"id\":\"test_stat\""), "Should contain rule ID")
        assertTrue(content.contains("\"text\":\"test code\""), "Should contain code snippet")
        assertTrue(content.contains("\"uri\":\"test.kt\""), "Should contain file path")
        assertTrue(content.contains("\"owner\":\"testOwner\""), "Should contain owner information")
        assertTrue(content.contains("\"owner\":\"codeStatOwner\""), "Should contain owner information")
        assertTrue(content.contains("\"module\":\"testModule\""), "Should contain module information")
    }

    private fun createTestStatsData(): StatsJsReportModel {
        val codeRefStatInfo = StatMetadata(
            key = "test_stat",
            description = "Test Stat",
            dataType = StatDataType.CODE_REFERENCES,
            extras = listOf(
                ExtraMetadata(
                    key = "extra_info",
                    type = ExtraDataType.STRING,
                    description = "Extra information for test stat"
                )
            )
        )

        val codeReferences = listOf(
            Stat.CodeReferencesStat.CodeReference(
                filePath = "test.kt",
                startLine = 1,
                endLine = 10,
                code = "test code",
                owner = "testOwner"
            ),
            Stat.CodeReferencesStat.CodeReference(
                filePath = "test2.kt",
                startLine = 5,
                endLine = 15,
                code = "test code 2",
                owner = "testOwner2"
            )
        )

        val statsByModule = mapOf(
            "module1" to mapOf(
                codeRefStatInfo.key to Stat.CodeReferencesStat(codeReferences)
            )
        )

        return StatsJsReportModel(
            statInfos = mapOf(codeRefStatInfo.key to codeRefStatInfo),
            statsByModule = statsByModule
        )
    }
}
