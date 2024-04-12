package com.squareup.invert

import com.squareup.invert.models.ConfigurationName
import org.gradle.api.Project

interface InvertIncludeConfigurationCalculator {

  /**
   * Whether or not a configuration should be included in the `invert` plugin's analysis.
   *
   * @param project [Project] which configurations should be computed for
   * @param configurationNames A [Collection] of all [ConfigurationName]s for this [Project].
   * @return The configuration names to analyze for this [Project]
   */
  fun invoke(
    project: Project,
    configurationNames: Collection<ConfigurationName>
  ): Collection<ConfigurationName>
}
