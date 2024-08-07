package com.squareup.invert

import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.internal.models.InvertCombinedCollectedData
import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.OwnerInfo
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

  val mavenRepoUrls = projectMetadata.mavenRepoUrls

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

  fun getProject(gradlePath: GradlePath): AllCollectedDataForProject? {
    return AllCollectedDataForProject(
      collectedDependencies = allCollectedData.collectedDependencies.firstOrNull { it.path == gradlePath }
        ?: CollectedDependenciesForProject(
          path = gradlePath,
          emptyMap(),
          emptyMap()
        ),
      collectedConfigurations = allCollectedData.collectedConfigurations.firstOrNull { it.path == gradlePath }
        ?: CollectedConfigurationsForProject(
          path = gradlePath,
          emptySet(),
          emptySet()
        ),
      collectedOwners = allCollectedData.collectedOwners.firstOrNull { it.path == gradlePath }
        ?: CollectedOwnershipForProject(
          path = gradlePath,
          ownerInfo = OwnerInfo("None"),
        ),
      collectedStats = allCollectedData.collectedStats.firstOrNull { it.path == gradlePath }
        ?: CollectedStatsForProject(
          path = gradlePath,
          emptyMap(),
          emptyMap()
        ),
      collectedPlugins = allCollectedData.collectedPlugins.firstOrNull { it.path == gradlePath }
        ?: CollectedPluginsForProject(
          path = gradlePath,
          emptyList()
        ),
    )
  }
}
