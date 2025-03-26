package com.squareup.invert.internal

import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import org.gradle.api.artifacts.result.ResolvedComponentResult

internal object InvertDependencyCollectors {

  /**
   * Inverts a Gradle Project Configuration -> Dependencies mapping
   * (i.e. "runtimeClasspath" -> [":common", ":models"] into a mapping of
   * DependencyId -> List<ConfigurationName>
   * (i.e. ":common" -> ["runtimeClasspath"], ":models" -> ["runtimeClasspath"])
   *
   * This inverted view of dependencies by configuration allow us to create
   * insights through the entire Gradle workspace.
   */
  private fun invertConfigurationToDependenciesData(
    collectedConfigurations: Set<CollectedDependenciesForConfiguration>
  ): Map<DependencyId, Set<ConfigurationName>> {
    val map = mutableMapOf<DependencyId, MutableSet<ConfigurationName>>()
    collectedConfigurations.forEach { collectedDataForConfiguration ->
      collectedDataForConfiguration.dependencyIds.forEach { dependencyId ->
        val configurations = map[dependencyId] ?: mutableSetOf()
        configurations.add(collectedDataForConfiguration.configuration)
        map[dependencyId] = configurations
      }
    }
    return map
  }

  /**
   * Computes dependency data for a single Gradle [Project].
   */
  fun computeCollectedDependenciesForProject(
    directDependencies: Map<ConfigurationName, Set<DependencyId>>,
    projectPath: String,
    transitiveDeps: Map<ConfigurationName, Set<DependencyId>>,
  ): CollectedDependenciesForProject {
    val collectedConfigurations = mutableSetOf<CollectedDependenciesForConfiguration>()
    transitiveDeps.forEach { (configurationName, transitives) ->
      collectedConfigurations.add(
        CollectedDependenciesForConfiguration(
          configuration = configurationName,
          dependencyIds = transitives
        ),
      )
    }

    return CollectedDependenciesForProject(
      path = projectPath,
      dependencies = invertConfigurationToDependenciesData(collectedConfigurations),
      directDependencies = directDependencies
    )
  }
}

/**
 * Resolves the configured Gradle [ResolvedComponentResult] into a full list of transitive
 * dependencies for this given [ConfigurationName] ("runtimeClasspath")
 */
fun Map<ConfigurationName, ResolvedComponentResult>.toTransitiveDeps():
    Map<ConfigurationName, Set<DependencyId>> {
  val transitiveDeps = mutableMapOf<ConfigurationName, Set<DependencyId>>()
  this.forEach { (configurationName, resolvedComponentResult) ->
    val collectedDependenciesForConfiguration = DependencyVisitor.traverseComponentDependencies(
      resolvedComponentResult
    )
    transitiveDeps[configurationName] = collectedDependenciesForConfiguration.map { it.name }.toSet()
  }
  return transitiveDeps
}
