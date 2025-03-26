package com.squareup.invert.models.js

import com.squareup.invert.models.GradlePluginId
import com.squareup.invert.models.ModulePath
import kotlinx.serialization.Serializable

/**
 * Model [JsReportFileKey.PLUGINS] for Invert Web Report
 */
@Serializable
data class PluginsJsReportModel(
  val plugins: Map<GradlePluginId, List<ModulePath>>,
  val modules: Map<ModulePath, List<GradlePluginId>>
)
