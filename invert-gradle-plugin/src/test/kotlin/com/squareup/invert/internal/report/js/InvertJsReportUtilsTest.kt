package com.squareup.invert.internal.report.js

import com.squareup.invert.models.InvertSerialization.InvertJsonPrettyPrint
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.OwnershipJsReportModel
import com.squareup.invert.models.js.StatTotalAndMetadata
import com.squareup.invert.models.js.StatsJsReportModel
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertEquals

class InvertJsReportUtilsTest {

  @Test
  fun testComputeGlobalTotals() {

    val MODULE_1 = "module1"
    val MODULE_2 = "module2"
    val OWNER_1 = "owner1"
    val OWNER_2 = "owner2"

    val codeRefStatInfo = StatMetadata(
      key = "code_reference_stat",
      title = "Code Reference Stat",
      dataType = StatDataType.CODE_REFERENCES,
    )

    val numericStatInfo = StatMetadata(
      key = "numeric_stat",
      title = "Numeric Stat",
      dataType = StatDataType.NUMERIC,
    )

    var codeReferenceCount = 0
    fun generateCodeReference(ownerName: OwnerName?): Stat.CodeReferencesStat.CodeReference {
      return Stat.CodeReferencesStat.CodeReference(
        filePath = "code${codeReferenceCount++}.kt",
        startLine = 1,
        endLine = 100,
        code = "code...",
        owner = ownerName
      )
    }

    val statsByModule = mutableMapOf<ModulePath, MutableMap<StatKey, Stat>>(
      MODULE_1 to mutableMapOf(
        codeRefStatInfo.key to Stat.CodeReferencesStat(
          listOf(
            generateCodeReference(null),
            generateCodeReference(OWNER_1),
            generateCodeReference(OWNER_2),
          )
        ),
        numericStatInfo.key to Stat.NumericStat(42)
      ),
      MODULE_2 to mutableMapOf(
        codeRefStatInfo.key to Stat.CodeReferencesStat(
          listOf(
            generateCodeReference(null),
            generateCodeReference(null),
            generateCodeReference(null),
          )
        ),
        numericStatInfo.key to Stat.NumericStat(20)
      )
    )

    val allProjectsStatsData = StatsJsReportModel(
      statInfos = listOf(codeRefStatInfo, numericStatInfo).associateBy { it.key },
      statsByModule = statsByModule
    )
    val collectedOwnershipInfo = OwnershipJsReportModel(
      teams = setOf(OWNER_1, OWNER_2),
      modules = mapOf(MODULE_1 to OWNER_1, MODULE_2 to OWNER_2)
    )

    val result: Map<String, StatTotalAndMetadata> =
      InvertJsReportUtils.computeGlobalTotals(allProjectsStatsData, collectedOwnershipInfo)

    assertEquals(
      """
{
    "code_reference_stat": {
        "metadata": {
            "key": "code_reference_stat",
            "title": "Code Reference Stat",
            "dataType": "CODE_REFERENCES"
        },
        "total": 6,
        "totalByOwner": {
            "owner1": 2,
            "owner2": 4
        }
    },
    "numeric_stat": {
        "metadata": {
            "key": "numeric_stat",
            "title": "Numeric Stat",
            "dataType": "NUMERIC"
        },
        "total": 62,
        "totalByOwner": {
            "owner1": 42,
            "owner2": 20
        }
    }
}
      """.trimIndent(),
      InvertJsonPrettyPrint.encodeToString(
        MapSerializer(String.serializer(), StatTotalAndMetadata.serializer()),
        result,
      ).also { json ->
        println(json)
      }
    )
  }
}