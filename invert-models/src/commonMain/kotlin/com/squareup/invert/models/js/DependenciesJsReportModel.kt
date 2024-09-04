package com.squareup.invert.models.js

import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.ModulePath
import kotlinx.serialization.Serializable

/**
 * Model [JsReportFileKey.INVERTED_DEPENDENCIES] for Invert Web Report
 */
@Serializable
data class DependenciesJsReportModel(
  val invertedDependencies: Map<
    DependencyId,
    Map<ModulePath, List<ConfigurationName>>
    >
) {

  fun getAllModulePaths(): List<ModulePath> {
    return invertedDependencies.keys.filter { it.startsWith(":") }.sorted()
  }

  fun getAllArtifactIds(): List<DependencyId> {
    return invertedDependencies.keys.filter { !it.startsWith(":") }.sorted()
  }
}
