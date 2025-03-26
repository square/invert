package com.squareup.invert.internal.models

import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.ModulePath
import kotlinx.serialization.Serializable

/**
 * Model for configuration related data collected for a single Gradle Project.
 *
 * This is used during the Invert Plugin's collect phase for a single Project.
 */
@Serializable
data class CollectedConfigurationsForProject(
  val modulePath: ModulePath,
  /** All Available Configurations */
  val allConfigurationNames: Set<ConfigurationName>,
  /** Configurations that were analyzed */
  val analyzedConfigurationNames: Set<ConfigurationName>,
)
