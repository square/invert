package com.squareup.invert.models.js

import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.ModulePath
import kotlinx.serialization.Serializable

/**
 * Model [JsReportFileKey.DIRECT_DEPENDENCIES] for Invert Web Report
 */
@Serializable
data class DirectDependenciesJsReportModel(
  val directDependencies: Map<
      ModulePath,
      Map<ConfigurationName, Set<DependencyId>>
      >
)
