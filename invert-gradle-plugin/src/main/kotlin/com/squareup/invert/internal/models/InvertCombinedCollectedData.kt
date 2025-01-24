package com.squareup.invert.internal.models

/**
 * A Combined view of all data collected for all projects.
 */
data class InvertCombinedCollectedData(
  val collectedConfigurations: Set<CollectedConfigurationsForProject>,
  val collectedDependencies: Set<CollectedDependenciesForProject>,
  val collectedOwners: Set<CollectedOwnershipForProject>,
  val collectedStats: Set<CollectedStatsForProject>,
  val collectedPlugins: Set<CollectedPluginsForProject>,
)
