package com.squareup.invert.models.js

import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.GradlePluginId
import kotlinx.serialization.Serializable

/**
 * Model [JsReportFileKey.PLUGINS] for Invert Web Report
 */
@Serializable
data class PluginsJsReportModel(
  val plugins: Map<GradlePluginId, List<GradlePath>>,
  val modules: Map<GradlePath, List<GradlePluginId>>
)
