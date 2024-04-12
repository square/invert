package com.squareup.invert.common

import com.squareup.invert.models.js.ConfigurationsJsReportModel
import com.squareup.invert.models.js.DependenciesJsReportModel
import com.squareup.invert.models.js.DirectDependenciesJsReportModel
import com.squareup.invert.models.js.HomeJsReportModel
import com.squareup.invert.models.js.JsReportFileKey
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.OwnershipJsReportModel
import com.squareup.invert.models.js.PluginsJsReportModel
import com.squareup.invert.models.js.StatsJsReportModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

/**
 * This is an agnostic way of accessing data collected by Invert, give a [JsReportFileKey]. It exposes this
 * data as [Flow]s. Note: Data is only loaded when a [Flow] is collected.  This is an optimization
 * in order to load data "just in time", and not eagerly.
 */
class CollectedDataRepo(
  private val coroutineDispatcher: CoroutineDispatcher,
  private val loadFileData: suspend (JsReportFileKey, CollectedDataRepo) -> Unit = { _, _ -> },
) {

  private val hasLoadedFile = mutableMapOf<JsReportFileKey, Boolean>()

  private suspend fun loadJsOfType(fileKey: JsReportFileKey) = withContext(coroutineDispatcher) {
    if (!hasLoadedFile.contains(fileKey)) {
      hasLoadedFile[fileKey] = true
      loadFileData(fileKey, this@CollectedDataRepo)
    }
  }

  private val _collectedPluginInfoReport: MutableStateFlow<PluginsJsReportModel?> =
    MutableStateFlow(null)
  val collectedPluginInfoReport: Flow<PluginsJsReportModel?> = _collectedPluginInfoReport
    .onEach { loadJsOfType(JsReportFileKey.PLUGINS) }

  private val _statsData = MutableStateFlow<StatsJsReportModel?>(null)
  val statsData: Flow<StatsJsReportModel?> = _statsData
    .onEach { loadJsOfType(JsReportFileKey.STATS) }

  private val _combinedReportData: MutableStateFlow<DependenciesJsReportModel?> =
    MutableStateFlow(null)
  val combinedReportData: Flow<DependenciesJsReportModel?> = _combinedReportData.onEach {
    loadJsOfType(JsReportFileKey.INVERTED_DEPENDENCIES)
  }

  private val _directDependenciesData: MutableStateFlow<DirectDependenciesJsReportModel?> =
    MutableStateFlow(null)
  val directDependenciesData: Flow<DirectDependenciesJsReportModel?> = _directDependenciesData.onEach {
    loadJsOfType(JsReportFileKey.DIRECT_DEPENDENCIES)
  }

  private val _ownersInfo: MutableStateFlow<OwnershipJsReportModel?> = MutableStateFlow(null)
  val ownersInfo: Flow<OwnershipJsReportModel?> = _ownersInfo.onEach {
    loadJsOfType(JsReportFileKey.OWNERS)
  }

  private val _reportMetadata: MutableStateFlow<MetadataJsReportModel?> = MutableStateFlow(null)
  val reportMetadata: Flow<MetadataJsReportModel?> = _reportMetadata.onEach {
    loadJsOfType(JsReportFileKey.METADATA)
  }

  private val _home: MutableStateFlow<HomeJsReportModel?> = MutableStateFlow(null)
  val home: Flow<HomeJsReportModel?> = _home.onEach {
    loadJsOfType(JsReportFileKey.HOME)
  }

  private val _configurations: MutableStateFlow<ConfigurationsJsReportModel?> = MutableStateFlow(null)
  val configurations: Flow<ConfigurationsJsReportModel?> = _configurations.onEach {
    loadJsOfType(JsReportFileKey.CONFIGURATIONS)
  }

  fun reportDataUpdated(reportData: DependenciesJsReportModel) {
    this._combinedReportData.value = reportData
  }

  fun directDependenciesDataUpdated(data: DirectDependenciesJsReportModel) {
    this._directDependenciesData.value = data
  }

  fun ownersUpdated(ownersInfo: OwnershipJsReportModel) {
    this._ownersInfo.value = ownersInfo
  }

  fun metadataUpdated(reportMetadata: MetadataJsReportModel) {
    this._reportMetadata.value = reportMetadata
  }

  fun pluginsUpdated(pluginInfo: PluginsJsReportModel) {
    this._collectedPluginInfoReport.value = pluginInfo
  }

  fun statsUpdated(statsData: StatsJsReportModel) {
    this._statsData.value = statsData
  }

  fun configurationsUpdated(configurationsData: ConfigurationsJsReportModel) {
    this._configurations.value = configurationsData
  }

  fun homeUpdated(data: HomeJsReportModel) {
    this._home.value = data
  }
}
