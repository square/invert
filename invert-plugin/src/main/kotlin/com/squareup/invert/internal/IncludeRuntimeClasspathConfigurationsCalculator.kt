package com.squareup.invert.internal

import com.squareup.invert.InvertIncludeConfigurationCalculator
import com.squareup.invert.models.ConfigurationName
import org.gradle.api.Project

/**
 * Default implementation which will include all configurations for a subproject
 */
object IncludeRuntimeClasspathConfigurationsCalculator : InvertIncludeConfigurationCalculator {
  override fun invoke(
    project: Project,
    configurationNames: Collection<ConfigurationName>
  ): Collection<ConfigurationName> {
    return configurationNames.filter { it.lowercase().endsWith("runtimeclasspath") }
  }
}
