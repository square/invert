package com.squareup.invert.models.js

import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.utils.BuildSystemUtils
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

  fun getAllModulePaths(buildSystem: BuildSystem): List<ModulePath> {
    return invertedDependencies.keys.filter {
      BuildSystemUtils.isSourceModule(buildSystem, it)
    }.sorted()
  }

  fun getAllArtifactIds(buildSystem: BuildSystem): List<DependencyId> {
    return invertedDependencies.keys.filter {
      BuildSystemUtils.isArtifact(buildSystem, it)
    }.sorted()
  }
}
