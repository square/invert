package navigation

import com.squareup.invert.common.CollectedDataRepo
import com.squareup.invert.common.Log
import com.squareup.invert.common.PerformanceAndTiming
import com.squareup.invert.models.InvertSerialization.InvertJson
import com.squareup.invert.models.js.CollectedStatTotalsJsReportModel
import com.squareup.invert.models.js.ConfigurationsJsReportModel
import com.squareup.invert.models.js.DependenciesJsReportModel
import com.squareup.invert.models.js.DirectDependenciesJsReportModel
import com.squareup.invert.models.js.HistoricalData
import com.squareup.invert.models.js.HomeJsReportModel
import com.squareup.invert.models.js.JsReportFileKey
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.OwnershipJsReportModel
import com.squareup.invert.models.js.PluginsJsReportModel
import com.squareup.invert.models.js.StatsJsReportModel
import externalLoadJavaScriptFile
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.builtins.ListSerializer

object RemoteJsLoadingProgress {
  val awaitingResults = MutableStateFlow<List<JsReportFileKey>>(listOf())

  fun getTimeoutForFileKey(jsReportFileKey: JsReportFileKey): Int {
    return when (jsReportFileKey) {
      JsReportFileKey.METADATA -> 10
      else -> 30
    } * 1_000
  }

  fun loadJavaScriptFile(fileKey: JsReportFileKey, callback: (String) -> Unit) {
    if (!awaitingResults.value.contains(fileKey)) {
      awaitingResults.value = awaitingResults.value.toMutableList().apply { add(fileKey) }
      Log.d("Loading $fileKey")
      val loadingTimeout = window.setTimeout(
        {
          window.alert("Timeout while fetching ${fileKey.description} data.")
        },
        getTimeoutForFileKey(fileKey)
      )
      externalLoadJavaScriptFile(fileKey.key) { json ->
        Log.d("Finished Loading $fileKey")
        window.clearTimeout(loadingTimeout)
        callback(json)
        awaitingResults.value = awaitingResults.value.toMutableList().apply { remove(fileKey) }
      }
    }
  }

  fun handleLoadedJsFile(collectedDataRepo: CollectedDataRepo, fileKey: JsReportFileKey, json: String) =
    PerformanceAndTiming.computeMeasureDurationBlocking("Deserializing $fileKey") {
      when (fileKey) {
        JsReportFileKey.INVERTED_DEPENDENCIES -> {
          collectedDataRepo.reportDataUpdated(
            InvertJson.decodeFromString(DependenciesJsReportModel.serializer(), json)
          )
        }

        JsReportFileKey.DIRECT_DEPENDENCIES -> {
          collectedDataRepo.directDependenciesDataUpdated(
            InvertJson.decodeFromString(DirectDependenciesJsReportModel.serializer(), json)
          )
        }

        JsReportFileKey.HOME -> {
          collectedDataRepo.homeUpdated(
            InvertJson.decodeFromString(HomeJsReportModel.serializer(), json)
          )
        }

        JsReportFileKey.PLUGINS -> {
          collectedDataRepo.pluginsUpdated(
            InvertJson.decodeFromString(PluginsJsReportModel.serializer(), json)
          )
        }

        JsReportFileKey.OWNERS -> {
          collectedDataRepo.ownersUpdated(
            InvertJson.decodeFromString(OwnershipJsReportModel.serializer(), json)
          )
        }

        JsReportFileKey.METADATA -> {
          collectedDataRepo.metadataUpdated(
            InvertJson.decodeFromString(MetadataJsReportModel.serializer(), json)
          )
        }

        JsReportFileKey.STATS -> {
          collectedDataRepo.statsUpdated(
            InvertJson.decodeFromString(StatsJsReportModel.serializer(), json)
          )
        }

        JsReportFileKey.CONFIGURATIONS -> {
          collectedDataRepo.configurationsUpdated(
            InvertJson.decodeFromString(ConfigurationsJsReportModel.serializer(), json)
          )
        }

        JsReportFileKey.STAT_TOTALS -> {
          collectedDataRepo.statTotalsUpdated(
            InvertJson.decodeFromString(CollectedStatTotalsJsReportModel.serializer(), json)
          )
        }

        JsReportFileKey.HISTORICAL_DATA -> collectedDataRepo.historicalDataUpdated(
          InvertJson.decodeFromString(ListSerializer(HistoricalData.serializer()), json)
        )
      }
    }
}