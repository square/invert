package com.squareup.invert.internal.report

import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.models.InvertSerialization.InvertJson
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class InvertJsonStreamsTest {

  private lateinit var testDir: File

  @BeforeTest
  fun setup() {
    testDir = File("build/test-reports/json-streams-test-${System.currentTimeMillis()}")
    testDir.mkdirs()
  }

  @AfterTest
  fun cleanup() {
    testDir.deleteRecursively()
  }

  @Test
  fun `streaming JSON is byte compatible and readable`() {
    val dependencyData = CollectedDependenciesForProject(
      path = ":unicode-ブランチ-🎉",
      dependencies = mapOf(
        "com.example:library:1.0.0" to setOf("implementation", "émoji"),
      ),
      directDependencies = mapOf(
        "implementation" to setOf("com.example:library:1.0.0"),
      ),
    )
    val serializer = CollectedDependenciesForProject.serializer()
    val expectedBytes = InvertJson.encodeToString(serializer, dependencyData).toByteArray()
    val file = File(testDir, "dependencies.json")

    InvertJsonStreams.write(file, serializer, dependencyData)

    assertContentEquals(expectedBytes, file.readBytes())
    assertEquals(expectedBytes.size.toLong(), InvertJsonStreams.encodedSizeInBytes(serializer, dependencyData))
    assertEquals(dependencyData, InvertReportFileUtils.buildModuleToFeaturesMap(file))
  }
}
