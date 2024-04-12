package com.squareup.invert.internal

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

internal object DependencyVisitor {

  /**
   * Traverses a dependency tree to collect all unique [Dependency] entries.
   */
  fun traverseComponentDependencies(
    resolvedComponentResult: ResolvedComponentResult,
  ): Set<Dependency> {
    val firstLevelDependencies = resolvedComponentResult
      .dependencies
      .filterIsInstance<ResolvedDependencyResult>()

    val dependencies = mutableSetOf<Dependency>()
    visit(dependencies, firstLevelDependencies)
    return dependencies.toSet()
  }

  private fun ResolvedDependencyResult.toDep(): Dependency? {
    return when (val componentIdentifier = selected.id) {
      is ProjectComponentIdentifier -> {
        Dependency.ModuleDependency(
          path = componentIdentifier.projectPath,
        )
      }

      is ModuleComponentIdentifier -> {
        Dependency.ArtifactDependency(
          group = componentIdentifier.group,
          artifact = componentIdentifier.module,
          version = componentIdentifier.version,
        )
      }

      else -> {
        null
      }
    }
  }

  private fun visit(
    reportData: MutableSet<Dependency>,
    resolvedDependencyResults: Collection<ResolvedDependencyResult>,
  ) {
    for (resolvedDependencyResult: ResolvedDependencyResult in resolvedDependencyResults) {
      resolvedDependencyResult.toDep()
        ?.let { dep: Dependency ->
          if (!reportData.contains(dep)) {
            reportData.add(dep)
            visit(
              reportData = reportData,
              resolvedDependencyResults = resolvedDependencyResult
                .selected
                .dependencies
                .filterIsInstance<ResolvedDependencyResult>(),
            )
          }
        }
    }
  }
}
