package com.squareup.invert

import com.squareup.invert.internal.models.InvertCombinedCollectedData
import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.Stat
import com.squareup.invert.models.js.MetadataJsReportModel

/**
 * A friendly layer on top of the combined data to allow aggregated [Stat]s to be calculated.
 */
class InvertAllCollectedDataRepo(
  private val allCollectedData: InvertCombinedCollectedData,
  private val projectMetadata: MetadataJsReportModel,
) {

  val httpsRemoteRepoUrlForCommit: String = "${projectMetadata.remoteRepoUrl}/blob/${projectMetadata.gitSha}"

  val projectPaths: Set<GradlePath> by lazy {
    mutableSetOf<String>().apply {
      allCollectedData.apply {
        collectedDependencies.forEach { add(it.path) }
        collectedConfigurations.forEach { add(it.path) }
        collectedOwners.forEach { add(it.path) }
        collectedStats.forEach { add(it.path) }
        collectedPlugins.forEach { add(it.path) }
      }
    }
  }

  fun getProject(gradlePath: GradlePath): AllCollectedDataForProject {
    return AllCollectedDataForProject(
      collectedDependencies = allCollectedData.collectedDependencies.first { it.path == gradlePath },
      collectedConfigurations = allCollectedData.collectedConfigurations.first { it.path == gradlePath },
      collectedOwners = allCollectedData.collectedOwners.first { it.path == gradlePath },
      collectedStats = allCollectedData.collectedStats.first { it.path == gradlePath },
      collectedPlugins = allCollectedData.collectedPlugins.first { it.path == gradlePath },
    )
  }
}
