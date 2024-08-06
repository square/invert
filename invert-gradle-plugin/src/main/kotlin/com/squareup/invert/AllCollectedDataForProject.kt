package com.squareup.invert

import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.CollectedStatsForProject

/**
 * Represents all the project specific data that was collected.
 */
data class AllCollectedDataForProject(
  val collectedConfigurations: CollectedConfigurationsForProject,
  val collectedDependencies: CollectedDependenciesForProject,
  val collectedOwners: CollectedOwnershipForProject,
  val collectedStats: CollectedStatsForProject,
  val collectedPlugins: CollectedPluginsForProject,
)
