package com.squareup.invert.internal.models

import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.ModulePath
import kotlinx.serialization.Serializable

/**
 * Model for dependency related data collected for a single Gradle Project.
 *
 * This is used during the Invert Plugin's collect phase for a single Project.
 */
@Serializable
data class CollectedDependenciesForProject(
  val path: ModulePath,
  val dependencies: Map<DependencyId, Set<ConfigurationName>>,
  val directDependencies: Map<ConfigurationName, Set<DependencyId>>,
)
