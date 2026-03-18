package com.squareup.invert

import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.internal.models.InvertCombinedCollectedData
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerInfo
import com.squareup.invert.models.Stat
import com.squareup.invert.models.js.AllOwners
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.OwnerDetails
import java.util.Collections

/**
 * A friendly layer on top of the combined data to allow aggregated [Stat]s to be calculated.
 */
class InvertAllCollectedDataRepo(
  private val allCollectedData: InvertCombinedCollectedData,
  private val projectMetadata: MetadataJsReportModel,
) {

  val httpsRemoteRepoUrlForCommit: String =
    "${projectMetadata.remoteRepoUrl}/blob/${projectMetadata.latestCommitGitSha}"

  val mavenRepoUrls = projectMetadata.artifactRepositories

  val projectPaths: Set<ModulePath> by lazy {
    mutableSetOf<String>().apply {
      allCollectedData.apply {
        collectedDependencies.forEach { add(it.path) }
        collectedConfigurations.forEach { add(it.modulePath) }
        collectedOwners.forEach { add(it.path) }
        collectedStats.forEach { add(it.path) }
        collectedPlugins.forEach { add(it.path) }
      }
    }
  }

  private val ownersSnapshot: AllOwners by lazy {
    AllOwners(
      ownerToDetails = Collections.unmodifiableMap(
        projectMetadata.owners.ownerToDetails.entries.associate { (ownerSlug, ownerDetails) ->
          ownerSlug to ownerDetails.asReadOnly()
        }
      )
    )
  }

  /**
   * Returns a read-only snapshot of ownership metadata keyed by owner slug.
   *
   * This is intended for stat collector aggregation pipelines that need to enrich output with
   * owner-level metadata (for example display name, contact, org, team id, or manager details)
   * without depending on internal report models.
   */
  fun getOwners(): AllOwners {
    return ownersSnapshot
  }

  fun getProject(modulePath: ModulePath): AllCollectedDataForProject? {
    return AllCollectedDataForProject(
      collectedDependencies = allCollectedData.collectedDependencies.firstOrNull { it.path == modulePath }
        ?: CollectedDependenciesForProject(
          path = modulePath,
          emptyMap(),
          emptyMap()
        ),
      collectedConfigurations = allCollectedData.collectedConfigurations.firstOrNull { it.modulePath == modulePath }
        ?: CollectedConfigurationsForProject(
          modulePath = modulePath,
          emptySet(),
          emptySet()
        ),
      collectedOwners = allCollectedData.collectedOwners.firstOrNull { it.path == modulePath }
        ?: CollectedOwnershipForProject(
          path = modulePath,
          ownerName = OwnerInfo.UNOWNED,
        ),
      collectedStats = allCollectedData.collectedStats.firstOrNull { it.path == modulePath }
        ?: CollectedStatsForProject(
          path = modulePath,
          emptyMap(),
          emptyMap()
        ),
      collectedPlugins = allCollectedData.collectedPlugins.firstOrNull { it.path == modulePath }
        ?: CollectedPluginsForProject(
          path = modulePath,
          emptyList()
        ),
    )
  }

  private fun OwnerDetails.asReadOnly(): OwnerDetails {
    return copy(metadata = Collections.unmodifiableMap(metadata.toMap()))
  }
}
