package com.squareup.invert.internal

import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId

/**
 * Intermediate data model used instead of a [Pair]
 */
internal data class CollectedDependenciesForConfiguration(
  val configuration: ConfigurationName,
  val dependencyIds: Set<DependencyId>,
)
