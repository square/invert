package com.squareup.invert.models.js

import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.GradlePath
import kotlinx.serialization.Serializable

/**
 * Model [JsReportFileKey.CONFIGURATIONS] for Invert Web Report
 *
 * Information about the Gradle Configurations available.
 */
@Serializable
data class ConfigurationsJsReportModel(
  val allConfigurationNames: Set<ConfigurationName>,
  val moduleToAllConfigurationNames: Map<GradlePath, Set<ConfigurationName>>,
  val moduleToAnalyzedConfigurationNames: Map<GradlePath, Set<ConfigurationName>>,
  val configurationNameToModules: Map<ConfigurationName, Set<GradlePath>>,
)
