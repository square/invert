package com.squareup.invert.internal.models

/**
 * A Combined view of all data collected for all projects.
 */
data class InvertCombinedCollectedData(
  val collectedConfigurations: List<CollectedConfigurationsForProject>,
  val collectedDependencies: List<CollectedDependenciesForProject>,
  val collectedOwners: List<CollectedOwnershipForProject>,
  val collectedStats: List<CollectedStatsForProject>,
  val collectedPlugins: List<CollectedPluginsForProject>,
)
