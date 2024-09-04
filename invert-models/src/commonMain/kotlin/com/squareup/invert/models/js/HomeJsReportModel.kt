package com.squareup.invert.models.js

import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.GradlePluginId
import kotlinx.serialization.Serializable

/**
 * Model [JsReportFileKey.HOME] for Invert Web Report
 *
 * A small subset of collected data needed to render the Invert report "home" page quickly.
 */
@Serializable
data class HomeJsReportModel(
  val modules: List<ModulePath>,
  val artifacts: List<DependencyId>,
  val plugins: List<GradlePluginId>,
)
