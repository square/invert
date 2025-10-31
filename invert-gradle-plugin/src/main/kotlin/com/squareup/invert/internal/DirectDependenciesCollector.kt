package com.squareup.invert.internal

import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency

internal class DirectDependenciesCollector(
  private val annotationProcessorNames: Set<String> = setOf("kapt", "ksp", "annotationProcessor"),
  private val kotlinCompilerPluginSubstring: String = "kotlinCompilerPluginClasspath",
) {
  fun collect(
    configurations: Set<Configuration>,
    filteredConfigurationNames: Set<String>,
  ): Map<ConfigurationName, Set<DependencyId>> {
    return configurations
      .filter { cfg ->
        filteredConfigurationNames.contains(cfg.name) ||
            annotationProcessorNames.contains(cfg.name) ||
            cfg.name.contains(kotlinCompilerPluginSubstring)
      }
      .associate { cfg ->
        cfg.name to cfg.allDependencies.mapNotNull { dependency ->
          when (dependency) {
            is ExternalDependency -> "${dependency.group}:${dependency.name}:${dependency.version}"
            is DefaultProjectDependency -> dependency.dependencyProject.path
            else -> null
          }
        }.toSortedSet()
      }
      .filterValues { it.isNotEmpty() }
  }
}



