package com.squareup.invert.internal

import com.squareup.invert.models.ConfigurationName
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.provider.Provider

/**
 * Utility method to get the Gradle [Project] configurations for a given project.
 *
 * For the root project, the buildscript classpath is important.
 */
internal val Project.projectConfigurations: ConfigurationContainer
  get() = if (isRootProject()) {
    buildscript.configurations
  } else {
    configurations
  }

/**
 * Utility method to get the root [ResolvedComponentResult] for a given configurationName
 */
internal fun ConfigurationContainer.getResolvedComponentResult(
  configurationName: ConfigurationName
): Provider<ResolvedComponentResult>? =
  this
    .findByName(configurationName)
    ?.incoming
    ?.resolutionResult
    ?.rootComponent

fun Project.isRootProject(): Boolean = this == this.rootProject
