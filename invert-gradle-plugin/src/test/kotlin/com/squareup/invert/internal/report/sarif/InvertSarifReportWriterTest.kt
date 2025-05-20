package com.squareup.invert.internal.report.sarif

import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.StatsJsReportModel
import org.gradle.internal.impldep.org.testng.Assert.assertEquals
import kotlin.test.Test
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
        val content = sarifFile.readText()
        assertTrue(content.contains("\"name\":\"Invert\""), "Should contain tool name")
        assertTrue(content.contains("\"version\":\"1.0.0\""), "Should contain tool version")
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
            description = "Test Description"
        )

        // Then
        assertTrue(testFile.exists(), "SARIF file should be created")
        val content = testFile.readText()
        assertTrue(content.contains("\"id\":\"test_stat\""), "Should contain rule ID")
        assertTrue(content.contains("\"text\":\"test code\""), "Should contain code snippet")
        assertTrue(content.contains("\"uri\":\"test.kt\""), "Should contain file path")
    }

    private fun createTestStatsData(): StatsJsReportModel {
        val codeRefStatInfo = StatMetadata(
            key = "test_stat",
            description = "Test Stat",
            dataType = StatDataType.CODE_REFERENCES
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